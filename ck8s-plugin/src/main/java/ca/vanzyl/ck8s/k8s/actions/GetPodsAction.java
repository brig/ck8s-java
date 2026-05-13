package ca.vanzyl.ck8s.k8s.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.common.Mapper;
import ca.vanzyl.ck8s.k8s.K8sClientFactory;
import ca.vanzyl.ck8s.k8s.K8sTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;

import static ca.vanzyl.ck8s.k8s.K8sTaskParams.GetPodsParams;

public class GetPodsAction implements K8sTaskAction<GetPodsParams> {

    private static final Logger log = LoggerFactory.getLogger(GetPodsAction.class);
    private static final Logger processLog = LoggerFactory.getLogger("processLog");

    @Override
    public Action action() {
        return Action.GET_PODS;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, GetPodsParams input) throws Exception {
        var namespace = input.namespace();
        var labelsFilter = input.labelsFilter();
        var dumpPods = input.dumpPods();

        try (KubernetesClient client = K8sClientFactory.create(input)) {
            FilterWatchListDeletable<Pod, PodList, PodResource> podOp = client.pods().inNamespace(namespace);
            if (labelsFilter != null && !labelsFilter.isEmpty()) {
                podOp = podOp.withLabels(labelsFilter);
            }

            var pods = podOp.list().getItems();

            log.info("Found {} pods in '{}' namespace", pods.size(), namespace);

            if (dumpPods) {
                processLog.info("{}", String.format("%-40s %-10s %-10s %-15s %-20s",
                        "NAME", "READY", "STATUS", "RESTARTS", "AGE"));

                for (var pod : pods) {
                    var name = pod.getMetadata().getName();

                    var total = pod.getSpec().getContainers().size();
                    var readyCount = pod.getStatus().getContainerStatuses() == null ? 0 :
                            pod.getStatus().getContainerStatuses().stream()
                                    .filter(cs -> Boolean.TRUE.equals(cs.getReady()))
                                    .count();
                    var ready = readyCount + "/" + total;

                    var status = pod.getStatus().getPhase();

                    var restarts = pod.getStatus().getContainerStatuses() == null ? 0 :
                            pod.getStatus().getContainerStatuses().stream()
                                    .mapToInt(cs -> cs.getRestartCount() == null ? 0 : cs.getRestartCount())
                                    .sum();

                    var age = formatAge(pod);

                    processLog.info("{}", String.format("%-40s %-10s %-10s %-15d %-20s",
                            name, ready, status, restarts, age));
                }
            }

            return TaskResult.success()
                    .value("pods", pods.stream().map(p -> Mapper.json().convertToMap(p)).toList());
        } catch (KubernetesClientException e) {
            log.error("❌ Failed to get pods in namespace '{}': {}", namespace, e.getMessage());
            return TaskResult.fail(e);
        }
    }

    private static String formatAge(Pod pod) {
        if (pod.getMetadata().getCreationTimestamp() == null) {
            return "-";
        }

        var d = Duration.between(OffsetDateTime.parse(pod.getMetadata().getCreationTimestamp()), OffsetDateTime.now());

        long seconds = d.getSeconds();
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return seconds + "s";
        } else if (minutes < 60) {
            return minutes + "m";
        } else if (hours < 24) {
            long remMinutes = minutes % 60;
            return remMinutes == 0 ? hours + "h" : hours + "h" + remMinutes + "m";
        } else {
            long remHours = hours % 24;
            return remHours == 0 ? days + "d" : days + "d" + remHours + "h";
        }
    }
}
