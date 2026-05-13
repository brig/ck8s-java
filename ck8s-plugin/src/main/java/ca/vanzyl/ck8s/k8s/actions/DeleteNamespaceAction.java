package ca.vanzyl.ck8s.k8s.actions;

import ca.vanzyl.ck8s.k8s.K8sClientFactory;
import ca.vanzyl.ck8s.k8s.K8sTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static ca.vanzyl.ck8s.k8s.K8sTaskParams.DeleteNamespaceParams;

public class DeleteNamespaceAction implements K8sTaskAction<DeleteNamespaceParams> {

    private static final Logger log = LoggerFactory.getLogger(DeleteNamespaceAction.class);

    @Override
    public Action action() {
        return K8sTaskAction.Action.DELETE_NAMESPACE;
    }

    @Override
    public TaskResult execute(Context context, DeleteNamespaceParams input) throws Exception {
        var namespace = input.namespace();

        try (KubernetesClient client = K8sClientFactory.create(input)) {
            var existingNamespace = client.namespaces().withName(namespace).get();
            if (existingNamespace == null) {
                log.warn("⚠️ Namespace '{}' does not exist. Nothing to delete.", namespace);
                return TaskResult.success();
            }

            log.info("Namespace '{}' delete initialized...", namespace);

            client.namespaces().withName(namespace).delete();

            client.namespaces()
                    .withName(namespace)
                    .waitUntilCondition(Objects::isNull, -1, TimeUnit.SECONDS);

            log.info("✅ Namespace '{}' deleted successfully.", namespace);

            return TaskResult.success();
        } catch (KubernetesClientException e) {
            log.error("❌ Failed to delete namespace '{}': {}", namespace, e.getMessage());
            return TaskResult.fail(e);
        }
    }
}
