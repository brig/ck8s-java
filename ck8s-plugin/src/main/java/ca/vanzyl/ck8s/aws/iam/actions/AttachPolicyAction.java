package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.AttachRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.AttachedPolicy;
import software.amazon.awssdk.services.iam.model.IamException;
import software.amazon.awssdk.services.iam.model.ListAttachedRolePoliciesRequest;

import javax.inject.Inject;
import java.util.List;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.AttachPolicyParams;

public class AttachPolicyAction extends IamTaskAction<AttachPolicyParams> {

    private final static Logger log = LoggerFactory.getLogger(AttachPolicyAction.class);

    @Inject
    public AttachPolicyAction(IamClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.ATTACH_POLICY;
    }

    @Override
    public TaskResult execute(Context context, AttachPolicyParams input) throws Exception {
        var roleName = input.roleName();
        var policyName = input.policyName();
        var policyArn = input.policyArn();

        try (var client = createClient(input)) {
            var attachedPolicies = attachedPolicies(client, roleName);
            if (attachedPolicies.contains(policyArn)) {
                log.info("✅ Policy '{}' is already attached to role '{}'", policyName, roleName);
                return TaskResult.success();
            }

            log.info("Attaching policy '{}' to role '{}'...", policyName, roleName);
            client.attachRolePolicy(AttachRolePolicyRequest.builder()
                    .roleName(roleName)
                    .policyArn(policyArn)
                    .build());

            log.info("✅ Successfully attached policy '{}' to role '{}'", policyName, roleName);

            return TaskResult.success();
        } catch (IamException e) {
            log.error("❌ Failed to attach policy '{}' to role '{}': {}", policyName, roleName, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }

    public static List<String> attachedPolicies(IamClient client, String roleName) {
        return client.listAttachedRolePoliciesPaginator(ListAttachedRolePoliciesRequest.builder()
                        .roleName(roleName)
                        .build()).attachedPolicies().stream().map(AttachedPolicy::policyArn)
                .toList();
    }
}
