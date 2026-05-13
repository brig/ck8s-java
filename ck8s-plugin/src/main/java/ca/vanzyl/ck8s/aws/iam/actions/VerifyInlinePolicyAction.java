package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.model.IamException;

import javax.inject.Inject;
import java.util.Set;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.PutRolePolicyParams;
import static ca.vanzyl.ck8s.aws.iam.actions.PutPolicyOrVerifyAction.inlinePolicyDocument;
import static ca.vanzyl.ck8s.utils.VerifyUtils.verifyJsonAttribute;

public class VerifyInlinePolicyAction extends IamTaskAction<PutRolePolicyParams> {

    private final static Logger log = LoggerFactory.getLogger(VerifyInlinePolicyAction.class);

    @Inject
    public VerifyInlinePolicyAction(IamClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.VERIFY_INLINE_POLICY;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, PutRolePolicyParams input) throws Exception {
        var roleName = input.roleName();
        var policyName = input.policyName();
        var policyDocument = input.policyDocument();

        try (var client = createClient(input)) {
            var currentDocument = inlinePolicyDocument(client, roleName, policyName);
            if (currentDocument == null) {
                log.error("❌ Inline policy '{}' for role '{}' does not exists", policyName, roleName);
                return TaskResult.fail("Inline policy does not exist");
            }

            var valid = verifyPolicy(new Policy(currentDocument), new Policy(policyDocument));
            if (!valid) {
                log.info("❌ Policy '{}' changed", roleName);
                return TaskResult.fail("Policy changed");
            }

            log.info("✅ Policy '{}' valid", policyName);

            return TaskResult.success();
        } catch (IamException e) {
            log.error("❌ Failed to verify policy '{}' for role '{}': {}", policyName, roleName, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }

    private boolean verifyPolicy(Policy existingPolicy, Policy newPolicy) {
        return verifyJsonAttribute("Policy document", existingPolicy.document(), newPolicy.document());
    }

    public record Policy (String document) {
    }
}
