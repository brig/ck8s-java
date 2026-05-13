package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;

import javax.inject.Inject;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.DeleteRoleParams;

public class DeleteRoleAction extends IamTaskAction<DeleteRoleParams> {

    private final static Logger log = LoggerFactory.getLogger(DeleteRoleAction.class);

    @Inject
    public DeleteRoleAction(IamClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.DELETE_ROLE;
    }

    @Override
    public TaskResult execute(Context context, DeleteRoleParams input) throws Exception {
        var roleName = input.roleName();

        try (var client = createClient(input)) {
            log.info("Deleting '{}' role...", roleName);

            if (GetRoleAction.getRole(client, roleName) == null) {
                log.info("✅ Role '{}' not found", roleName);
                return TaskResult.success();
            }

            detachManagedPolicies(client, roleName);

            deleteInlinePolicies(client, roleName);

            removeRoleFromInstanceProfiles(client, roleName);

            deleteRole(client, roleName);

            return TaskResult.success();
        } catch (IamException e) {
            log.error("❌ Failed to delete role '{}': {}", roleName, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        } catch (RuntimeException e) {
            return TaskResult.fail(e);
        }
    }

    private static void detachManagedPolicies(IamClient iamClient, String roleName) {
        try {
            var listResponse = iamClient.listAttachedRolePoliciesPaginator(
                    ListAttachedRolePoliciesRequest.builder()
                            .roleName(roleName)
                            .build());

            for (AttachedPolicy policy : listResponse.attachedPolicies()) {
                log.info("Detaching policy '{}'", policy.policyArn());

                try {
                    iamClient.detachRolePolicy(DetachRolePolicyRequest.builder()
                            .roleName(roleName)
                            .policyArn(policy.policyArn())
                            .build());
                } catch (NoSuchEntityException e) {
                    log.warn("Role '{}' not found or has no attached managed policies: {}", roleName, e.getMessage());
                }
            }
            log.info("✅ All managed policies detached");
        } catch (NoSuchEntityException e) {
            log.warn("Role '{}' not found or has no attached managed policies: {}", roleName, e.getMessage());
        } catch (IamException e) {
            log.error("❌ Failed to detach managed policies: {}", e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to detach managed policies", e);
        }
    }

    private static void deleteInlinePolicies(IamClient client, String roleName) {
        try {
            var inlinePolicies = client.listRolePoliciesPaginator(ListRolePoliciesRequest.builder()
                    .roleName(roleName)
                    .build());

            for (var name : inlinePolicies.policyNames()) {
                deleteInlinePolicy(client, roleName, name);
            }

            log.info("✅ All inline policies deleted");
        } catch (NoSuchEntityException e) {
            log.warn("Role '{}' does not exist or has no inline policies: {}", roleName, e.getMessage());
        } catch (IamException e) {
            log.error("❌ Failed to delete inline policies from role '{}': {}", roleName, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to delete inline policies", e);
        }
    }

    private static void deleteInlinePolicy(IamClient client, String roleName, String policyName) {
        try {
            client.deleteRolePolicy(DeleteRolePolicyRequest.builder()
                    .roleName(roleName)
                    .policyName(policyName)
                    .build());
        } catch (NoSuchEntityException e) {
            log.warn("Inline policy '{}' or role '{}' not found: {}", policyName, roleName, e.getMessage());
        }
    }

    private static void removeRoleFromInstanceProfiles(IamClient client, String roleName) {
        try {
            var response = client.listInstanceProfilesForRolePaginator(ListInstanceProfilesForRoleRequest.builder()
                    .roleName(roleName)
                    .build());

            for (InstanceProfile profile : response.instanceProfiles()) {
                String profileName = profile.instanceProfileName();

                try {
                    client.removeRoleFromInstanceProfile(RemoveRoleFromInstanceProfileRequest.builder()
                            .instanceProfileName(profileName)
                            .roleName(roleName)
                            .build());

                    log.info("✅ Removed role '{}' from instance profile '{}'", roleName, profileName);
                } catch (NoSuchEntityException e) {
                    log.warn("Instance profile '{}' does not exist or has no role: {}", profileName, e.getMessage());
                }

                if (profile.roles().size() == 1 && profile.roles().get(0).roleName().equals(roleName)) {
                    try {
                        client.deleteInstanceProfile(DeleteInstanceProfileRequest.builder()
                                .instanceProfileName(profileName)
                                .build());

                        log.info("✅ Deleted instance profile {}", profileName);
                    } catch (NoSuchEntityException e) {
                        log.warn("Instance profile '{}' does not exist: {}", profileName, e.getMessage());
                    }
                }
            }
        } catch (IamException e) {
            log.error("❌ Failed to remove '{}' role from instance profiles: {}", roleName, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to remove role from instance profiles", e);
        }
    }

    private static void deleteRole(IamClient iamClient, String roleName) {
        try {
            iamClient.deleteRole(DeleteRoleRequest.builder()
                    .roleName(roleName)
                    .build());
        } catch (NoSuchEntityException e) {
            log.warn("Role does not exist '{}'", roleName);
        } catch (IamException e) {
            log.error("❌ Failed to delete '{}' role: {}", roleName, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to delete role", e);
        }
    }
}
