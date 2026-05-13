package ca.vanzyl.ck8s.k8s;

import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

/**
 * A wrapper task for fabric8's Kubernetes client.
 * See {@link KubernetesClientTask.Action} for available actions.
 */
@Named("kubeClient")
public class KubernetesClientTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(KubernetesClientTask.class);

    // truncate the output after a certain size
    private static final int MAX_OUTPUT_SIZE = 10 * 1024 * 1024; // 10 MB

    @Override
    public TaskResult execute(Variables input) throws Exception {
        var action = Action.fromInput(input);
        switch (action) {
            case GET:
                return doGet(input);
            case EXEC:
                return doExec(input);
        }

        return Task.super.execute(input);
    }

    private TaskResult doGet(Variables input) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private TaskResult doExec(Variables input) {
        var namespace = input.assertString("namespace");
        var podName = input.assertString("podName");
        var saveOutput = input.getBoolean("saveOutput", false);
        var saveErrorOutput = input.getBoolean("saveErrorOutput", false);
        var cmd = input.assertList("cmd").stream()
                .filter(Objects::nonNull)
                .map(s -> {
                    if (!(s instanceof String)) {
                        throw new IllegalArgumentException("'cmd' must be a list of strings, got a %s instead: %s".formatted(s.getClass().getSimpleName(), s));
                    }
                    return (String) s;
                })
                .toArray(String[]::new);

        // TODO pipe line-by-line into the logger
        var stdout = new TruncatingOutputStream();
        var stderr = new TruncatingOutputStream();

        try (var client = createClient(input);
             var watch = client.pods()
                     .inNamespace(namespace)
                     .withName(podName)
                     .writingOutput(stdout)
                     .writingError(stderr)
                     .exec(cmd)) {

            var exitCode = watch.exitCode().join();

            var stdoutString = stdout.toString();
            log.info("STDOUT:\n{}", stdoutString);

            if (exitCode == null || exitCode != 0) {
                throw new RuntimeException("Command failed with exit code %s: %s".formatted(exitCode, stderr.toString()));
            }

            var result = TaskResult.success();
            if (saveOutput) {
                result.value("output", stdoutString);
            }
            if (saveErrorOutput) {
                result.value("errorOutput", stderr.toString());
            }

            return result;
        }
    }

    private static KubernetesClient createClient(Variables input) {
        var builder = new KubernetesClientBuilder();

        var env = input.getMap("env", Map.of());
        if (env != null) {
            var kubeconfig = env.get("KUBECONFIG");
            if (kubeconfig != null) {
                try {
                    var path = Paths.get(kubeconfig.toString());
                    var config = Files.readString(path);
                    builder.withConfig(config);
                    log.info("Using KUBECONFIG: {}", kubeconfig);
                } catch (InvalidPathException e) {
                    throw new IllegalArgumentException("Invalid env.KUBECONFIG path: %s".formatted(kubeconfig));
                } catch (IOException e) {
                    throw new RuntimeException("Error reading KUBECONFIG: %s".formatted(kubeconfig), e);
                }
            }
        }

        return builder.build();
    }

    enum Action {
        GET,
        EXEC;

        final static String AVAILABLE_ACTIONS = Stream.of(Action.values())
                .map(Enum::name)
                .collect(joining(", "));

        static Action fromInput(Variables input) {
            var action = input.assertString("Missing 'action'. Available actions: %s".formatted(Action.AVAILABLE_ACTIONS), "action")
                    .toLowerCase();
            try {
                return valueOf(action.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown action '%s'. Available actions: %s".formatted(action, AVAILABLE_ACTIONS));
            }
        }
    }

    static class TruncatingOutputStream extends ByteArrayOutputStream {
        // when we hand of the stream to KubernetesClient, we expect it to use only the write(byte[], int, int) method

        @Override
        public void write(byte[] b, int off, int len) {
            if (size() + len > MAX_OUTPUT_SIZE) {
                super.write(b, off, MAX_OUTPUT_SIZE - size());
            } else {
                super.write(b, off, len);
            }
        }

        @Override
        public synchronized void write(int b) {
            throw new IllegalStateException("should not be called");
        }

        @Override
        public void write(byte[] b) {
            throw new IllegalStateException("should not be called");
        }

        @Override
        public synchronized String toString() {
            var s = super.toString(UTF_8);
            if (size() >= MAX_OUTPUT_SIZE) {
                return s + "...[cut]";
            }
            return s;
        }
    }
}
