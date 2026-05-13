package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;
import software.amazon.awssdk.services.iam.paginators.ListEntitiesForPolicyIterable;

import javax.inject.Inject;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.DeletePolicyParams;
import static ca.vanzyl.ck8s.aws.iam.actions.CreatePolicyAction.cleanupPolicyVersions;
import static ca.vanzyl.ck8s.aws.iam.actions.CreatePolicyAction.getPolicy;

public class DeletePolicyAction extends IamTaskAction<DeletePolicyParams> {

    private final static Logger log = LoggerFactory.getLogger(DeletePolicyAction.class);

    @Inject
    public DeletePolicyAction(IamClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.DELETE_POLICY;
    }

    @Override
    public TaskResult execute(Context context, DeletePolicyParams input) throws Exception {
        var policyArn = input.policyArn();
        var detachFromResources = input.detachFromResources();

        log.info("Deleting managed policy '{}'...", policyArn);

        try (var client = createClient(input)) {
            if (getPolicy(client, policyArn) == null) {
                log.info("✅ Policy '{}' does not exist", policyArn);
                return TaskResult.success();
            }

            if (detachFromResources) {
                detachPolicyFromAllEntities(client, policyArn);
            }

            cleanupPolicyVersions(client, policyArn);

            deletePolicy(client, policyArn);

            return TaskResult.success();
        } catch (IamException e) {
            log.error("❌ Failed to delete managed policy '{}': {}", policyArn, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }

    private static void deletePolicy(IamClient client, String policyArn) {
        try {
            client.deletePolicy(DeletePolicyRequest.builder()
                    .policyArn(policyArn)
                    .build());
            log.info("✅ Managed policy '{}' deleted successfully", policyArn);
        } catch (NoSuchEntityException e) {
            log.info("✅ Policy '{}' does not exist, nothing to delete", policyArn);
        }
    }

    private static void detachPolicyFromAllEntities(IamClient client, String policyArn) {
        ListEntitiesForPolicyIterable entitiesResponse;

        try {
            entitiesResponse = client.listEntitiesForPolicyPaginator(
                    ListEntitiesForPolicyRequest.builder()
                            .policyArn(policyArn)
                            .build());
        } catch (NoSuchEntityException e) {
            log.info("Policy '{}' does not exist or is already detached", policyArn);
            return;
        }

        entitiesResponse.policyRoles().forEach(role -> detachPolicy(client, role.roleName(), policyArn));
        entitiesResponse.policyGroups().forEach(group -> detachGroupPolicy(client, group.groupName(), policyArn));
        entitiesResponse.policyUsers().forEach(user -> detachPolicyFromUser(client, user.userName(), policyArn));
    }

    private static void detachPolicy(IamClient client, String roleName, String policyArn) {
        log.info("Detaching policy '{}' from role '{}'", policyArn, roleName);

        try {
            client.detachRolePolicy(DetachRolePolicyRequest.builder()
                    .roleName(roleName)
                    .policyArn(policyArn)
                    .build());
            log.info("✅ Successfully detached policy {} from role {}", policyArn, roleName);
        } catch (NoSuchEntityException e) {
            log.info("Policy '{}' does not exists or does not attached to role '{}'", policyArn, roleName);
        }
    }

    private static void detachGroupPolicy(IamClient client, String groupName, String policyArn) {
        log.info("Detaching policy '{}' from group '{}'", policyArn, groupName);

        try {
            client.detachGroupPolicy(
                    DetachGroupPolicyRequest.builder()
                            .groupName(groupName)
                            .policyArn(policyArn)
                            .build()
            );

            log.info("✅ Successfully detached group policy {}", policyArn);
        } catch (NoSuchEntityException e) {
            log.info("Policy '{}' does not exist or does not attached to group '{}': {}", policyArn, groupName, e.getMessage());
        }
    }

    private static void detachPolicyFromUser(IamClient client, String user, String policyArn) {
        log.info("Detaching policy '{}' from user '{}'", policyArn, user);

        try {
            client.detachUserPolicy(
                    DetachUserPolicyRequest.builder()
                            .userName(user)
                            .policyArn(policyArn)
                            .build()
            );
            log.info("✅ Successfully detached policy '{}'", policyArn);
        } catch (NoSuchEntityException e) {
            log.info("Policy '{}' does not exist or does not attached to user '{}': {}", policyArn, user, e.getMessage());
        }
    }
}
