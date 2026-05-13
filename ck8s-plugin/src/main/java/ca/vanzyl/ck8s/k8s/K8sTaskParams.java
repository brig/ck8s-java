package ca.vanzyl.ck8s.k8s;

import ca.vanzyl.ck8s.actions.ActionInput;

import java.nio.file.Path;
import java.util.Map;

public interface K8sTaskParams extends ActionInput {

    BaseParams baseParams();

    record BaseParams(Path kubeConfigPath) {
    }

    record CreateNamespaceParams(BaseParams baseParams, String namespace, Map<String, String> labels)
            implements K8sTaskParams {

        public CreateNamespaceParams {
            labels = (labels == null) ? Map.of() : Map.copyOf(labels);
        }
    }

    record DeleteNamespaceParams(BaseParams baseParams, String namespace)
            implements K8sTaskParams {
    }

    record ListEventsParams(BaseParams baseParams, String namespace, boolean dumpEvents)
            implements K8sTaskParams {
    }

    record ExistsNamespaceParams(BaseParams baseParams, String namespace)
            implements K8sTaskParams {
    }

    record GetSecretParams(BaseParams baseParams, String namespace, String secretName)
            implements K8sTaskParams {
    }

    record CreateSecretParams(BaseParams baseParams, String namespace, String secretName, Map<String, String> data)
            implements K8sTaskParams {
    }

    record GetPodsParams(BaseParams baseParams, String namespace, Map<String, String> labelsFilter, boolean dumpPods)
            implements K8sTaskParams {
    }

    record PodLogsParams(BaseParams baseParams, String namespace, String podName, boolean includeInitContainers, boolean includeEphemeral, int tailingLines)
            implements K8sTaskParams {
    }
}
