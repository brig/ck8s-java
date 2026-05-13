package ca.vanzyl.ck8s.k8s.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.k8s.K8sClientFactory;
import ca.vanzyl.ck8s.k8s.K8sTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EphemeralContainer;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static ca.vanzyl.ck8s.k8s.K8sTaskParams.PodLogsParams;

public class PodLogsAction implements K8sTaskAction<PodLogsParams> {

    private static final Logger log = LoggerFactory.getLogger(PodLogsAction.class);
    private static final Logger processLog = LoggerFactory.getLogger("processLog");

    @Override
    public Action action() {
        return Action.POD_LOGS;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, PodLogsParams input) throws Exception {
        var namespace = input.namespace();
        var podName = input.podName();

        try (KubernetesClient client = K8sClientFactory.create(input)) {
            var pod = client.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .get();

            if (pod == null) {
                log.info("Pod '{}' not found in '{}' namespace", podName, namespace);
                return TaskResult.success();
            }

            var containers = containers(pod, input.includeInitContainers(), input.includeEphemeral());
            for (var container : containers) {
                if (containers.size() > 1) {
                    processLog.info("----- Processing container '{}' -----\n", container);
                }

                var podRes = client.pods()
                        .inNamespace(namespace)
                        .withName(podName)
                        .inContainer(container)
                        .tailingLines(input.tailingLines());

                try (var reader = new BufferedReader(new InputStreamReader(podRes.getLogInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("ERROR") || line.contains("Exception ")) {
                            processLog.error("{}", line);
                        } else {
                            processLog.info("{}", line);
                        }
                    }
                }
            }

            return TaskResult.success();
        } catch (KubernetesClientException e) {
            log.error("❌ Failed to get pods '{}' logs in namespace '{}': {}", podName, namespace, e.getMessage());
            return TaskResult.fail(e);
        }
    }

    private static List<String> containers(Pod pod, boolean includeInit, boolean includeEphemeral) {
        var spec = pod.getSpec();
        if (spec == null) {
            return List.of();
        }

        Stream<String> main = spec.getContainers() == null
                ? Stream.empty()
                : spec.getContainers().stream().map(Container::getName).filter(Objects::nonNull);

        Stream<String> init = includeInit && spec.getInitContainers() != null
                ? spec.getInitContainers().stream().map(Container::getName).filter(Objects::nonNull)
                : Stream.empty();

        Stream<String> eph = includeEphemeral && spec.getEphemeralContainers() != null
                ? spec.getEphemeralContainers().stream().map(EphemeralContainer::getName).filter(Objects::nonNull)
                : Stream.empty();

        return Stream.concat(main, Stream.concat(init, eph)).toList();
    }
}
