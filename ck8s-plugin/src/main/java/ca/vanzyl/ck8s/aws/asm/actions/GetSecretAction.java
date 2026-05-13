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

public class GetSecretAction extends AsmTaskAction<AsmTaskParams.GetSecretParams> {

    private static final Logger log = LoggerFactory.getLogger(GetSecretAction.class);

    @Inject
    public GetSecretAction(AsmClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.GET_SECRET;
    }

    @Override
    public TaskResult execute(Context context, AsmTaskParams.GetSecretParams input) throws Exception {
        var name = input.name();

        try (var client = createClient(input)) {
            var response = client.getSecretValue(r -> r.secretId(name));

            return TaskResult.success()
                    .value("name", response.name())
                    .value("secretValue", response.secretString());
        } catch (ResourceNotFoundException e) {
            log.error("❌ Secret '{}' not found", name);
            return TaskResult.fail(e);
        } catch (SecretsManagerException e) {
            log.error("❌ Failed to get secret '{}': {}", name, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }
}