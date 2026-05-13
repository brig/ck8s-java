package ca.vanzyl.ck8s.k8s.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.k8s.K8sClientFactory;
import ca.vanzyl.ck8s.k8s.K8sTaskAction;
import ca.vanzyl.ck8s.k8s.K8sTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class ExistsNamespaceAction implements K8sTaskAction<K8sTaskParams.ExistsNamespaceParams> {

    private static final Logger log = LoggerFactory.getLogger(ExistsNamespaceAction.class);

    @Override
    public Action action() {
        return Action.NAMESPACE_EXISTS;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, K8sTaskParams.ExistsNamespaceParams input) throws Exception {
        var namespace = input.namespace();

        try (var client = K8sClientFactory.create(input)) {
            var existingNamespace = client.namespaces().withName(namespace).get();
            return TaskResult.success()
                    .value("exists", existingNamespace != null);
        } catch (KubernetesClientException e) {
            log.error("❌ Failed to check namespace '{}': {}", namespace, e.getMessage());
            return TaskResult.fail(e);
        }
    }
}
