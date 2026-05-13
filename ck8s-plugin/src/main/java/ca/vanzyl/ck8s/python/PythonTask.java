package ca.vanzyl.ck8s.python;

import ca.vanzyl.ck8s.aws.CredentialsProvider;
import ca.vanzyl.ck8s.command.CliCommand;
import ca.vanzyl.ck8s.command.CommandTask;
import ca.vanzyl.ck8s.common.IOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Task for running Python scripts in virtual environment and with installed dependencies.
 * Usage:
 *   task: python
 *   in:
 *     script: path/to/script.py
 *     args:
 *       - "first"
 *       - "second with space"
 *     venv: false
 *     envvars: {}
 * Input variables:
 *  - script: string, required, path to Python script to execute
 *  - args: array of strings, optional (defaults to empty), list of script arguments
 *  - venv: bool, optional (defaults to true), should we create a virtual environment before
 *  running the script
 *  - reuseVenv: bool, optional (defaults to true), should we reuse an existing virtual environment based on the
 *  script name or venvName; if true, the virtual environment is created only once and reused for all subsequent runs
 *  of the script with the same name or for all script executions with the same venvName; otherwise, a new virtual
 *  environment is created for each run and removed after the script execution
 *  - venvName: string, optional, name of the virtual environment to create; allows to reuse the same virtual
 *  environment for multiple scripts; has no effect if reuseVenv is false
 *  - envars: map of strings, optional (defaults to empty), map of environment variables to
 *  pass to the script.
 * If a file named `requirements.txt` or 'pyproject.toml' exists in the directory containing the script,
 * pip is used to install dependencies listed in a file. Relative dependencies are resolved starting
 * from the directory containing the script.
 * Python script is executed without copying it, so any relative imports should just work.
 * You should always use venv for scripts that install external dependencies, otherwise pip
 * tries to install dependencies in a default (system global) environment.
 */
@Named("python")
public class PythonTask
    implements Task
{
    private final static Logger logger = LoggerFactory.getLogger(PythonTask.class);
    private final Path workDir;
    private final FileService fileService;
    private final CredentialsProvider credentialsProvider;

    @Inject
    public PythonTask(Context ctx, CredentialsProvider credentialsProvider)
    {
        this.workDir = ctx.workingDirectory();
        this.fileService = ctx.fileService();
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public TaskResult.SimpleResult execute(Variables input)
            throws Exception {
        PythonTaskParams params = new PythonTaskParams(input);
        var envParams = CommandTask.enrichEnvWithAwsCredentials(credentialsProvider, params.envars());

        Path script = createScript(params);

        List<String> args = List.of("bash", script.toAbsolutePath().toString());
        CliCommand command = new CliCommand(args, workDir, envParams, false, false);
        try {
            CliCommand.Result result = command.execute();
            if (result.getCode() != 0) {
                throw new UserDefinedException(String.format(
                        "The script '%s' failed to execute correctly: %s",
                        params.script(), result.getStderr()));
            }

            Map<String, Object> resultVars = new HashMap<>();
            // TODO: common parts for cmd task and this task
            if (params.responseFile() != null) {
                Path responsePath = workDir.resolve(params.responseFile());
                if (Files.exists(responsePath)) {
                    String responseContent = new String(Files.readAllBytes(workDir.resolve(params.responseFile())));
                    if (params.responseFile().endsWith(".json")) {
                        Map<String, Object> response = new ObjectMapper().readValue(responseContent, Map.class);
                        resultVars.put("response", response);
                    }
                    else if (params.responseFile().endsWith(".txt")) {
                        resultVars.put("response", responseContent);
                    }
                }
            }

            return TaskResult.success()
                    .values(resultVars);
        }
        catch (Exception e) {
            logger.error(
                    "Error running script '{}' with env '{}': {}",
                    params.script(), envParams, e.getMessage());
            throw e;
        }
    }

    private Path createScript(PythonTaskParams params)
            throws IOException
    {
        logger.info("Running script: {}", params.script());
        String scriptContent = resourceAsString("python-script-template.sh");
        String args = params.args().stream()
                .map(arg -> String.format("ARGS+=('%s')", arg))
                .collect(Collectors.joining(System.lineSeparator()));
        scriptContent = scriptContent
                .replace("%WORK_DIR%", workDir.toString())
                .replace("%SCRIPT%", params.script())
                .replace("%VENV%", params.venv() ? "true" : "")
                .replace("%REUSE_VENV%", params.reuseVenv() ? "true" : "")
                .replace("%VENV_NAME%", params.venvName())
                .replace("%ARGS%", args);
        if (params.debug(false)) {
            logger.info("SCRIPT:\n{}", scriptContent);
        }
        Path scriptPath = fileService.createTempFile("command", ".sh");
        Files.write(
                scriptPath, scriptContent.getBytes(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
        return scriptPath;
    }

    private String resourceAsString(String resource) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
            return IOUtils.read(is);
        }
    }
}
