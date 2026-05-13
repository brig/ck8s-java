package ca.vanzyl.ck8s.aws.asm.actions;

import ca.vanzyl.ck8s.aws.asm.AsmClientFactory;
import ca.vanzyl.ck8s.aws.asm.AsmTaskAction;
import ca.vanzyl.ck8s.aws.asm.AsmTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import javax.inject.Inject;

public class DeleteSecretAction extends AsmTaskAction<AsmTaskParams.DeleteSecretParams> {

    private static final Logger log = LoggerFactory.getLogger(DeleteSecretAction.class);

    @Inject
    public DeleteSecretAction(AsmClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.DELETE_SECRET;
    }

    @Override
    public TaskResult execute(Context context, AsmTaskParams.DeleteSecretParams input) throws Exception {
        var name = input.name();

        try (var client = createClient(input)) {
            client.deleteSecret(r -> r.secretId(name)
                    .forceDeleteWithoutRecovery(false));

            log.info("✅ Secret '{}' scheduled for deletion", name);
            return TaskResult.success()
                    .value("name", name);
        } catch (ResourceNotFoundException e) {
            log.info("Secret '{}' not found. Nothing to delete.", name);
            return TaskResult.success()
                    .value("name", name);
        } catch (SecretsManagerException e) {
            log.error("❌ Failed to delete secret '{}': {}", name, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }
}