package ca.vanzyl.ck8s.k8s.actions;

import ca.vanzyl.ck8s.k8s.K8sClientFactory;
import ca.vanzyl.ck8s.k8s.K8sTaskAction;
import ca.vanzyl.ck8s.k8s.K8sTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateNamespaceAction implements K8sTaskAction<K8sTaskParams.CreateNamespaceParams> {

    private static final Logger log = LoggerFactory.getLogger(CreateNamespaceAction.class);

    @Override
    public Action action() {
        return K8sTaskAction.Action.CREATE_NAMESPACE;
    }

    @Override
    public TaskResult execute(Context context, K8sTaskParams.CreateNamespaceParams input) throws Exception {
        var namespace = input.namespace();
        var labels = input.labels();

        try (var client = K8sClientFactory.create(input)) {
            var existingNamespace = client.namespaces().withName(namespace).get();
            if (existingNamespace != null) {
                log.info("✅ Namespace '{}' already exists.", namespace);

                if (!labels.isEmpty()) {
                    var updatedNamespace = new NamespaceBuilder(existingNamespace)
                            .editMetadata()
                            .addToLabels(labels)
                            .endMetadata()
                            .build();
                    client.resource(updatedNamespace).update();

                    log.info("✅ Labels updated successfully for '{}'.", namespace);
                }
            } else {
                var newNamespace = new NamespaceBuilder()
                        .withNewMetadata()
                        .withName(namespace)
                        .addToLabels(labels)
                        .endMetadata()
                        .build();

                client.resource(newNamespace).create();

                log.info("✅ Namespace '{}' created.", namespace);
            }

            return TaskResult.success();
        } catch (KubernetesClientException e) {
            log.error("❌ Failed to create namespace '{}': {}", namespace, e.getMessage());
            return TaskResult.fail(e);
        }
    }
}
