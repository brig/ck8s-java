package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import ca.vanzyl.ck8s.aws.iam.IamTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;

import javax.inject.Inject;
import java.util.List;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.CreateRoleParams;

public class CreateRoleAction extends IamTaskAction<CreateRoleParams> {

    private final static Logger log = LoggerFactory.getLogger(CreateRoleAction.class);

    @Inject
    public CreateRoleAction(IamClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.CREATE_ROLE;
    }

    @Override
    public TaskResult execute(Context context, IamTaskParams.CreateRoleParams input) throws Exception {
        var roleName = input.roleName();
        var trustPolicy = input.trustPolicy();
        var tags = input.tags();

        dumpInput(input);

        try (var client = createClient(input)) {
            if (GetRoleAction.getRole(client, roleName) != null) {
                log.info("Role '{}' exists. Updating it...", roleName);
                updateTrustPolicy(client, roleName, trustPolicy);
                updateTags(client, roleName, tags);
            } else {
                log.info("Role '{}' does not exists. Creating it...", roleName);
                createRole(client, roleName, trustPolicy, tags);
            }

            return TaskResult.success();
        } catch (IamException e) {
            log.error("❌ Failed to create role '{}': {}", roleName, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        } catch (RuntimeException e) {
            return TaskResult.fail(e);
        }
    }

    public static void dumpInput(CreateRoleParams input) {
        if (input.baseParams().debug()) {
            log.info("Trust policy:\n{}", input.trustPolicy());
            log.info("Tags:\n{}", input.tags());
        }
    }

    public static Role createRole(IamClient client, String roleName, String trustPolicy, List<Tag> tags) {
        try {
            var createRoleResponse = client.createRole(CreateRoleRequest.builder()
                    .roleName(roleName)
                    .assumeRolePolicyDocument(trustPolicy)
                    .tags(tags)
                    .build());

            log.info("✅ Role '{}' created successfully: {}", roleName, createRoleResponse.role().arn());

            log.info("Waiting until role exists...");
            client.waiter().waitUntilRoleExists(builder -> builder.roleName(roleName));

            log.info("✅ Role '{}' is now available", roleName);

            return createRoleResponse.role();
        } catch (IamException e) {
            log.error("❌ Failed to create role '{}': {}", roleName, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to create role: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    private void updateTrustPolicy(IamClient client, String roleName, String trustPolicy) {
        try {
            client.updateAssumeRolePolicy(UpdateAssumeRolePolicyRequest.builder()
                    .roleName(roleName)
                    .policyDocument(trustPolicy)
                    .build());

            log.info("✅ Trust policy updated successfully");
        } catch (IamException e) {
            log.error("❌ Failed to update trust policy for role '{}': {}", roleName, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to update trust policy: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    private void updateTags(IamClient client, String roleName, List<Tag> tags) {
        try {
            client.tagRole(r -> r.roleName(roleName)
                    .tags(tags));

            log.info("✅ Tags updated successfully");
        } catch (IamException e) {
            log.error("❌ Failed to update tags for role '{}': {}", roleName, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to update tags: " + e.awsErrorDetails().errorMessage(), e);
        }
    }
}
