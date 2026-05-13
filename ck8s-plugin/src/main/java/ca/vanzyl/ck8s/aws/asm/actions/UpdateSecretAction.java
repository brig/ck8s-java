package ca.vanzyl.ck8s.aws.asm.actions;

import ca.vanzyl.ck8s.aws.asm.AsmClientFactory;
import ca.vanzyl.ck8s.aws.asm.AsmTaskAction;
import ca.vanzyl.ck8s.aws.asm.AsmTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import javax.inject.Inject;

public class UpdateSecretAction extends AsmTaskAction<AsmTaskParams.UpdateSecretParams> {

    private static final Logger log = LoggerFactory.getLogger(UpdateSecretAction.class);

    @Inject
    public UpdateSecretAction(AsmClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.UPDATE_SECRET;
    }

    @Override
    public TaskResult execute(Context context, AsmTaskParams.UpdateSecretParams input) throws Exception {
        var name = input.name();

        try (var client = createClient(input)) {
            client.updateSecret(r -> r.secretId(name)
                    .secretString(input.secretString()));

            log.info("✅ Secret '{}' updated", name);
            return TaskResult.success()
                    .value("name", name);
        } catch (SecretsManagerException e) {
            log.error("❌ Failed to update secret '{}': {}", name, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }
}