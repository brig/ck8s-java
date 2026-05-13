package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import ca.vanzyl.ck8s.aws.iam.state.IamInlinePolicy;
import ca.vanzyl.ck8s.aws.iam.state.IamState;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.PutRolePolicyParams;
import static ca.vanzyl.ck8s.aws.iam.actions.PutPolicyOrVerifyAction.verifyPolicyDocument;

public class PutPolicyOrVerifyPreviewAction extends IamTaskAction<PutRolePolicyParams> {

    private final static Logger log = LoggerFactory.getLogger(PutPolicyOrVerifyPreviewAction.class);

    private final IamState state;

    @Inject
    public PutPolicyOrVerifyPreviewAction(IamClientFactory clientFactory, IamState state) {
        super(clientFactory);
        this.state = state;
    }

    @Override
    public Action action() {
        return Action.PUT_ROLE_POLICY_OR_VERIFY;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.PREVIEW);
    }

    @Override
    public TaskResult execute(Context context, PutRolePolicyParams input) throws Exception {
        var roleName = input.roleName();
        var policyName = input.policyName();
        var policyDocument = input.policyDocument();

        var role = state.role(input.baseParams(), roleName);
        if (role == null) {
            return TaskResult.fail("Role '" + roleName + "' not found");
        }

        var statePolicy = state.inlinePolicy(input.baseParams(), roleName, policyName);
        if (statePolicy != null) {
            log.info("[PREVIEW] Inline policy '{}' for role '{}' exists. Verifying it...", policyName, roleName);
            var valid = verifyPolicyDocument(statePolicy.document(), policyDocument);
            if (!valid) {
                return TaskResult.fail("Inline policy document differs from current policy");
            }
            log.info("[PREVIEW] ✅ Inline policy valid");
        } else {
            log.info("[PREVIEW] Inline policy '{}' for role '{}' does not exists. Creating it...", policyName, roleName);

            state.put(roleName, new IamInlinePolicy(roleName, policyName, policyDocument));
        }

        return TaskResult.success();
    }
}
