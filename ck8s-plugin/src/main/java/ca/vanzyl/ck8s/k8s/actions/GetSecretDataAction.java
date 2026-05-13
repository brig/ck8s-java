package ca.vanzyl.ck8s.k8s.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.k8s.K8sClientFactory;
import ca.vanzyl.ck8s.k8s.K8sTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static ca.vanzyl.ck8s.k8s.K8sTaskParams.GetSecretParams;

public class GetSecretDataAction implements K8sTaskAction<GetSecretParams> {

    private static final Logger log = LoggerFactory.getLogger(GetSecretDataAction.class);

    @Override
    public Action action() {
        return Action.GET_SECRET_DATA;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, GetSecretParams input) throws Exception {
        var namespace = input.namespace();
        var secretName = input.secretName();

        try (KubernetesClient client = K8sClientFactory.create(input)) {
            var secret = client.secrets()
                    .inNamespace(namespace)
                    .withName(secretName)
                    .get();

            if (secret != null) {
                return TaskResult.success()
                        .value("data", secret.getData());
            }

            return TaskResult.success();
        } catch (KubernetesClientException e) {
            log.error("❌ Failed to get secret '{}' namespace '{}': {}", secretName, namespace, e.getMessage());
            return TaskResult.fail(e);
        }
    }
}
