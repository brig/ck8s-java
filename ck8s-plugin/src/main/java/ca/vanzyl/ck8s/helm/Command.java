package ca.vanzyl.ck8s.helm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class Command {

    private final static Logger log = LoggerFactory.getLogger(Command.class);

    private final Path workDir;
    private final Map<String, String> envVars;

    public Command(Path workDir, Map<String, String> envVars) {
        this.workDir = workDir;
        this.envVars = envVars;
    }

    public Result execute(List<String> cmd, Long timeout, TimeUnit unit, boolean silent, boolean debug) throws Exception {
        var pb = new ProcessBuilder(cmd)
                .directory(workDir.toFile());

        pb.environment().putAll(envVars);

        if (debug) {
            log.info("Execute '{}' in {}", String.join(" ", cmd), workDir);
            log.info("\t using env: {}", envVars);
        }

        var p = pb.start();

        var executor = Executors.newCachedThreadPool();
        try {
            var stderr = executor.submit(new StreamReader(silent, p.getErrorStream()));
            var stdout = executor.submit(new StreamReader(silent, p.getInputStream()));

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
            return new Result(code, stdout.get(), stderr.get());
        } finally {
            try { p.getInputStream().close(); } catch (Exception ignore) {}
            try { p.getErrorStream().close(); } catch (Exception ignore) {}
            try { p.getOutputStream().close(); } catch (Exception ignore) {}

            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }

    private static void kill(Process proc) {
        if (!proc.isAlive()) {
            return;
        }
        try {
            proc.getOutputStream().close();
        } catch (Exception ignore) {
        }

        proc.descendants().forEach(ProcessHandle::destroy);
    }

    public record Result(int code, String stdout, String stderr) {
    }

    static class StreamReader implements Callable<String> {

        private final boolean silent;
        private final InputStream in;

        private StreamReader(boolean silent, InputStream in) {
            this.silent = silent;
            this.in = in;
        }

        @Override
        public String call() throws Exception {
            var sb = new StringBuilder();

            try (var reader = new BufferedReader(new InputStreamReader(in))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (!silent) {
                        log(line);
                    }

                    sb.append(removeAnsiColors(line))
                            .append(System.lineSeparator());
                }
            }

            return sb.toString();
        }

        private static void log(String s) {
            System.out.print("\u001b[34mhelm\u001b[0m: ");
            System.out.print(s);
            System.out.println();
        }

        private static String removeAnsiColors(String s) {
            return s.replaceAll("\u001B\\[[;\\d]*m", "");
        }
    }
}
