package ca.vanzyl.ck8s.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

// Replace with Airlift
public class CliCommand {

    private final static Logger logger = LoggerFactory.getLogger(CliCommand.class);

    private final boolean saveOutput;
    private final boolean saveError;
    private final Path workDir;
    private final List<String> args;
    private final Map<String, String> envars;

    public CliCommand(List<String> args, Path workDir, Map<String, String> envars, boolean saveOutput, boolean saveError) {
        this.workDir = workDir;
        this.args = args;
        this.envars = envars;
        this.saveOutput = saveOutput;
        this.saveError = saveError;
    }

    public Result execute() throws Exception {
        return execute(Executors.newCachedThreadPool());
    }

    public Result execute(Long timeout, TimeUnit unit) throws Exception {
        return execute(timeout, unit, Executors.newCachedThreadPool());
    }

    public Result execute(ExecutorService executor) throws Exception {
        return execute(null, null, executor);
    }

    public Result execute(Long timeout, TimeUnit unit, ExecutorService executor) throws Exception
    {
        ProcessBuilder pb = new ProcessBuilder(args).directory(workDir.toFile());
        Map<String, String> combinedEnv = new HashMap<>(envars);
        pb.environment().putAll(combinedEnv);
        Process p = pb.start();
        Future<String> stderr = executor.submit(new StreamReader("OUT", saveError, p.getErrorStream()));
        Future<String> stdout = executor.submit(new StreamReader("OUT", saveOutput, p.getInputStream()));
        int code;
        if (timeout != null && unit != null) {
            if (!p.waitFor(timeout, unit)) {
                kill(p);
                code = -42;
            } else {
                code = p.exitValue();
            }
        } else {
            code = p.waitFor();
        }
        executor.shutdown();
        return new Result(code, stdout.get(), stderr.get());
    }

    private static class StreamReader
            implements Callable<String>
    {
        private final String logPrefix;
        private final boolean saveOutput;
        private final InputStream in;

        private StreamReader(String logPrefix, boolean saveOutput, InputStream in)
        {
            this.logPrefix = logPrefix;
            this.saveOutput = saveOutput;
            this.in = in;
        }

        @Override
        public String call()
                throws Exception
        {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (saveOutput) {
                        sb.append(line).append(System.lineSeparator());
                    }
                    logger.info("{}: {}", logPrefix, line);
                }
            }
            return sb.toString();
        }
    }

    public static class Result
    {

        private final int code;
        private final String stdout;
        private final String stderr;

        public Result(int code, String stdout, String stderr)
        {
            this.code = code;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public int getCode()
        {
            return code;
        }

        public String getStdout()
        {
            return stdout;
        }

        public String getStderr()
        {
            return stderr;
        }
    }

    private static void kill(Process proc) {
        if (!proc.isAlive()) {
            return;
        }

        proc.descendants().forEach(t -> {
            sigQuit(t.pid());
            sleep(15000);
            t.destroy();
        });
    }

    private static void sigQuit(long pid) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("kill", "-3", String.valueOf(pid));
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info("sigQuit [{}] -> ok", pid);
            } else {
                logger.info("sigQuit [{}] -> error", pid);
            }
        } catch (IOException | InterruptedException e) {

        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
