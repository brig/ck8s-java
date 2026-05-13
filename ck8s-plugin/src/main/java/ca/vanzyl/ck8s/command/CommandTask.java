package ca.vanzyl.ck8s.command;

import ca.vanzyl.ck8s.aws.CredentialsProvider;
import ca.vanzyl.ck8s.common.Mapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.runtime.v2.runner.SensitiveDataHolder;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Named("command")
public class CommandTask
        implements Task
{

    private final static Logger logger = LoggerFactory.getLogger(CommandTask.class);

    private final Path workDir;
    private final boolean defaultDebug;
    private final FileService fileService;
    private final boolean withErrorHandler;
    private final CredentialsProvider credentialsProvider;

    @Inject
    public CommandTask(Context ctx, CredentialsProvider credentialsProvider)
    {
        this.workDir = ctx.workingDirectory();
        this.defaultDebug = ctx.processConfiguration().debug();
        this.fileService = ctx.fileService();
        this.withErrorHandler = ctx.variables().getBoolean("commandTaskWithErrorHandler", false);
        this.credentialsProvider = credentialsProvider;
    }

    private static void assertEnvVars(Map<String, String> envars)
    {
        if (envars == null) {
            return;
        }

        envars.forEach((key, value) -> {
            if (value == null) {
                throw new RuntimeException("Env variable '" + key + "' with null value. Remove this variable or set value");
            }
        });
    }

    @Override
    public TaskResult execute(Variables input)
            throws Exception
    {
        var params = new CommandTaskParams(input);
        var envParams = enrichEnvWithAwsCredentials(credentialsProvider, params.envars());

        var debug = params.debug(defaultDebug);
        var runScript = createRunScript(input.getBoolean("withErrorHandler", withErrorHandler), workDir, params.run());
        if (debug) {
            dumpParams(params, envParams, runScript);
        }

        assertEnvVars(envParams);

        var args = Arrays.asList("bash", runScript.toAbsolutePath().toString());
        var command = new CliCommand(args, workDir, envParams, params.saveOutput(), params.saveError());
        CliCommand.Result result;
        try {
            result = command.execute(params.timeout(), TimeUnit.SECONDS);
        }
        catch (Exception e) {
            if (!debug) {
                dumpParams(params, envParams, runScript);
            }
            logger.error("error execution command: {}", e.getMessage());
            throw e;
        }

        if (result.getCode() != 0) {
            if (!debug) {
                dumpParams(params, envParams, runScript);
            }

            throw new UserDefinedException(String.format("Non-zero command exit code: %d", result.getCode()));
        }

        var resultVars = new HashMap<String, Object>();
        if (params.saveOutput()) {
            resultVars.put("output", result.getStdout());
        }

        if (params.responseFile() != null) {
            var responsePath = workDir.resolve(params.responseFile());
            if (Files.exists(responsePath)) {
                var responseContent = new String(Files.readAllBytes(workDir.resolve(params.responseFile())));
                if (params.responseFile().endsWith(".json")) {
                    resultVars.put("response", new ObjectMapper().readValue(responseContent, Map.class));
                } else if (params.responseFile().endsWith(".txt")) {
                    resultVars.put("response", responseContent);
                } else if (params.responseFile().endsWith(".yaml")) {
                    resultVars.put("response", Mapper.yaml().readMap(responseContent));
                }
            }
        }

        return TaskResult.success()
                .values(resultVars);
    }

    private static void dumpParams(CommandTaskParams params, Map<String, String> envParams, Path runScript) {
        logger.info("command:\n{}", params.run());
        logger.info("env: {}", envParams);
        logger.info("runScript: {}", runScript);
    }

    private Path createRunScript(boolean withErrorHandler, Path workDir, String cmd)
            throws IOException
    {

        String scriptContent;
        if (withErrorHandler) {
            try (InputStream is = CommandTask.class.getResourceAsStream("/command-script-template.sh")) {
                if (is == null) {
                    throw new RuntimeException("Can't find script template. This is most like a bug");
                }

                try (InputStreamReader isr = new InputStreamReader(is);
                        BufferedReader reader = new BufferedReader(isr)) {
                    scriptContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                }
            }

            scriptContent = scriptContent.replace("%WORK_DIR%", workDir.toString())
                    .replace("%CMD%", cmd);
        }
        else {
            scriptContent = "#!/usr/bin/env bash\n" +
                    "cd " + workDir + "\n" +
                    cmd;
        }

        Path scriptPath = fileService.createTempFile("command", ".sh");
        Files.write(scriptPath, scriptContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
        return scriptPath;
    }

    public static Map<String, String> enrichEnvWithAwsCredentials(CredentialsProvider credentialsProvider, Map<String, String> inputEnv) {
        var sessionCredentials = credentialsProvider.getSessionCredentials();
        if (sessionCredentials == null) {
            return inputEnv;
        }

        var result = new HashMap<>(inputEnv);
        result.put("AWS_ACCESS_KEY_ID", sessionCredentials.accessKeyId());
        result.put("AWS_SECRET_ACCESS_KEY", sessionCredentials.secretAccessKey());
        result.put("AWS_SESSION_TOKEN", sessionCredentials.sessionToken());

        SensitiveDataHolder.getInstance().add(sessionCredentials.secretAccessKey());
        SensitiveDataHolder.getInstance().add(sessionCredentials.sessionToken());

        return result;
    }
}
