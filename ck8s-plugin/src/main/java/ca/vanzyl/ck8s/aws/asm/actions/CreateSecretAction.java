package ca.vanzyl.ck8s.aws.asm.actions;

import ca.vanzyl.ck8s.aws.asm.AsmClientFactory;
import ca.vanzyl.ck8s.aws.asm.AsmTaskAction;
import ca.vanzyl.ck8s.aws.asm.AsmTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.model.ResourceExistsException;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;
import software.amazon.awssdk.services.secretsmanager.model.Tag;

import javax.inject.Inject;

public class CreateSecretAction extends AsmTaskAction<AsmTaskParams.CreateSecretParams> {

    private static final Logger log = LoggerFactory.getLogger(CreateSecretAction.class);

    @Inject
    public CreateSecretAction(AsmClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.CREATE_SECRET;
    }

    @Override
    public TaskResult execute(Context context, AsmTaskParams.CreateSecretParams input) throws Exception {
        var name = input.name();
        var tags = input.tags().entrySet().stream()
                .map(e -> Tag.builder().key(e.getKey()).value(e.getValue()).build())
                .toList();

        try (var client = createClient(input)) {
            try {
                client.createSecret(r -> {
                    r.name(name)
                            .secretString(input.secretString());
                    if (!tags.isEmpty()) {
                        r.tags(tags);
                    }
                });
                log.info("✅ Secret '{}' created", name);
            } catch (ResourceExistsException e) {
                log.info("Secret '{}' already exists. Updating...", name);
                client.updateSecret(r -> r.secretId(name)
                        .secretString(input.secretString()));
                if (!tags.isEmpty()) {
                    client.tagResource(r -> r.secretId(name).tags(tags));
                }
                log.info("✅ Secret '{}' updated", name);
            }
            return TaskResult.success()
                    .value("name", name);
        } catch (SecretsManagerException e) {
            log.error("❌ Failed to create/update secret '{}': {}", name, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }
}