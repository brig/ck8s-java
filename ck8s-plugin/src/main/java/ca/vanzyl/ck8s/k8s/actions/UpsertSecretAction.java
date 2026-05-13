package ca.vanzyl.ck8s.k8s.actions;

import ca.vanzyl.ck8s.k8s.K8sClientFactory;
import ca.vanzyl.ck8s.k8s.K8sTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ca.vanzyl.ck8s.k8s.K8sTaskParams.CreateSecretParams;

public class UpsertSecretAction implements K8sTaskAction<CreateSecretParams> {

    private static final Logger log = LoggerFactory.getLogger(UpsertSecretAction.class);

    @Override
    public Action action() {
        return Action.UPSERT_SECRET;
    }

    @Override
    public TaskResult execute(Context context, CreateSecretParams input) throws Exception {
        var namespace = input.namespace();
        var secretName = input.secretName();

        try (KubernetesClient client = K8sClientFactory.create(input)) {
            var secret = new SecretBuilder()
                    .withNewMetadata()
                        .withName(secretName)
                        .withNamespace(namespace)
                    .endMetadata()
                    .withType("Opaque")
                    .withData(input.data())
                    .build();

            var resource = client.secrets()
                    .inNamespace(namespace)
                    .resource(secret);

            resource
                    .forceConflicts()
                    .serverSideApply();

            return TaskResult.success();
        } catch (KubernetesClientException e) {
            log.error("❌ Failed to upsert secret '{}' namespace '{}': {}", secretName, namespace, e.getMessage());
            return TaskResult.fail(e);
        }
    }
}
