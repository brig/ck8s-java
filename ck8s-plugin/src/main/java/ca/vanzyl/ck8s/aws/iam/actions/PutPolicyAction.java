package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.model.IamException;
import software.amazon.awssdk.services.iam.model.PutRolePolicyRequest;

import javax.inject.Inject;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.PutRolePolicyParams;

public class PutPolicyAction extends IamTaskAction<PutRolePolicyParams> {

    private final static Logger log = LoggerFactory.getLogger(PutPolicyAction.class);

    @Inject
    public PutPolicyAction(IamClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.PUT_ROLE_POLICY;
    }

    @Override
    public TaskResult execute(Context context, PutRolePolicyParams input) throws Exception {
        var roleName = input.roleName();
        var policyName = input.policyName();
        var policyDocument = input.policyDocument();

        dumpInput(input);

        try (var client = createClient(input)) {
            log.info("Putting inline policy '{}' to role '{}'...", policyName, roleName);

            client.putRolePolicy(PutRolePolicyRequest.builder()
                    .roleName(roleName)
                    .policyName(policyName)
                    .policyDocument(policyDocument)
                    .build());

            log.info("✅ Successfully putted inline policy '{}' to role '{}'", policyName, roleName);

            return TaskResult.success();
        } catch (IamException e) {
            log.error("❌ Failed to put policy '{}' to role '{}': {}", policyName, roleName, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }

    public static void dumpInput(PutRolePolicyParams input) {
        if (input.baseParams().debug()) {
            log.info("Policy:\n{}", input.policyDocument());
        }
    }
}
