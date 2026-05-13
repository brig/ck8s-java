package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import ca.vanzyl.ck8s.aws.iam.state.IamInlinePolicy;
import ca.vanzyl.ck8s.aws.iam.state.IamState;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;

import javax.inject.Inject;
import java.util.Set;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.PutRolePolicyParams;
import static ca.vanzyl.ck8s.aws.iam.actions.PutPolicyAction.dumpInput;

public class PutPolicyPreviewAction extends IamTaskAction<PutRolePolicyParams> {

    private final IamState state;

    @Inject
    public PutPolicyPreviewAction(IamClientFactory clientFactory, IamState state) {
        super(clientFactory);
        this.state = state;
    }

    @Override
    public Action action() {
        return Action.PUT_ROLE_POLICY;
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

        dumpInput(input);

        var role = state.role(input.baseParams(), roleName);
        if (role == null) {
            return TaskResult.fail("Role '" + roleName + "' not found");
        }

        // just to load current version
        state.inlinePolicy(input.baseParams(), roleName, policyName);

        state.put(roleName, new IamInlinePolicy(roleName, policyName, policyDocument));

        return TaskResult.success();
    }
}
