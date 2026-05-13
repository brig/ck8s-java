package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import ca.vanzyl.ck8s.aws.iam.state.IamRole;
import ca.vanzyl.ck8s.aws.iam.state.IamState;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;

import javax.inject.Inject;
import java.util.Set;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.AttachPolicyParams;

public class AttachPolicyPreviewAction extends IamTaskAction<AttachPolicyParams> {

    private final IamState state;

    @Inject
    public AttachPolicyPreviewAction(IamClientFactory clientFactory, IamState state) {
        super(clientFactory);
        this.state = state;
    }

    @Override
    public Action action() {
        return Action.ATTACH_POLICY;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.PREVIEW);
    }

    @Override
    public TaskResult execute(Context context, AttachPolicyParams input) throws Exception {
        var roleName = input.roleName();
        var policyName = input.policyName();
        var policyArn = input.policyArn();

        var role = state.role(input.baseParams(), roleName);
        if (role == null) {
            return TaskResult.fail("Role '" + roleName + "' not found");
        }

        var policy = state.managedPolicy(input.baseParams(), policyArn);
        if (policy == null) {
            return TaskResult.fail("Policy '" + policyName + "' not found");
        }

        state.put(IamRole.builder().from(role)
                .addAttachedPolicyArns(policyArn)
                .build());

        return TaskResult.success();
    }
}
