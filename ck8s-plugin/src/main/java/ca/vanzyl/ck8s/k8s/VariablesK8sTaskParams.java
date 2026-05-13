package ca.vanzyl.ck8s.k8s;

import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

public final class VariablesK8sTaskParams {

    private static final String KUBECONFIG_KEY = "kubeconfig";
    private static final String NAMESPACE_KEY = "namespace";
    private static final String LABELS_KEY = "labels";
    private static final String SECRET_KEY = "secret";
    private static final String DATA_KEY = "data";
    private static final String POD_NAME_KEY = "podName";

    public static K8sTaskParams.CreateNamespaceParams createNamespace(Variables variables) {
        return new K8sTaskParams.CreateNamespaceParams(
                baseParams(variables),
                namespace(variables),
                labels(variables)
        );
    }

    public static K8sTaskParams.DeleteNamespaceParams deleteNamespace(Variables variables) {
        return new K8sTaskParams.DeleteNamespaceParams(
                baseParams(variables),
                namespace(variables)
        );
    }

    public static K8sTaskParams.ListEventsParams listEvents(Variables variables) {
        return new K8sTaskParams.ListEventsParams(
                baseParams(variables),
                namespace(variables),
                variables.getBoolean("dumpEvents", false)
        );
    }

    public static K8sTaskParams.ExistsNamespaceParams existsNamespace(Variables variables) {
        return new K8sTaskParams.ExistsNamespaceParams(
                baseParams(variables),
                namespace(variables)
        );
    }

    public static K8sTaskParams.GetPodsParams getPods(Variables variables) {
        return new K8sTaskParams.GetPodsParams(
                baseParams(variables),
                namespace(variables),
                labels(variables),
                variables.getBoolean("dumpPods", false)
        );
    }

    public static K8sTaskParams.PodLogsParams podLogs(Variables variables) {
        return new K8sTaskParams.PodLogsParams(
                baseParams(variables),
                namespace(variables),
                podName(variables),
                variables.getBoolean("includeInitContainers", false),
                variables.getBoolean("includeEphemeral", false),
                variables.getInt("tailingLines", 100)
        );
    }

    public static K8sTaskParams.GetSecretParams getSecret(Variables variables) {
        return new K8sTaskParams.GetSecretParams(
                baseParams(variables),
                namespace(variables),
                secret(variables)
        );
    }

    public static K8sTaskParams.CreateSecretParams createSecret(Variables variables) {
        return new K8sTaskParams.CreateSecretParams(
                baseParams(variables),
                namespace(variables),
                secret(variables),
                data(variables)
        );
    }

    private static K8sTaskParams.BaseParams baseParams(Variables variables) {
        return new K8sTaskParams.BaseParams(kubeConfigPath(variables));
    }

    private static Path kubeConfigPath(Variables variables) {
        var configPath = variables.getString(KUBECONFIG_KEY);
        if (configPath == null) {
            return null;
        }

        var path = Path.of(configPath);
        if (Files.notExists(path)) {
            throw new UserDefinedException("kubeconfig file not found: " + path);
        }
        return path;
    }

    private static String namespace(Variables variables) {
        return variables.assertString(NAMESPACE_KEY);
    }

    private static String podName(Variables variables) {
        return variables.assertString(POD_NAME_KEY);
    }

    private static String secret(Variables variables) {
        return variables.assertString(SECRET_KEY);
    }

    private static Map<String, String> data(Variables variables) {
        return variables.assertMap(DATA_KEY);
    }

    private static Map<String, String> labels(Variables variables) {
        return variables.getMap(LABELS_KEY, Map.of()).entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toUnmodifiableMap(
                        entry -> String.valueOf(entry.getKey()),
                        entry -> String.valueOf(entry.getValue())
                ));
    }
}
