package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import ca.vanzyl.ck8s.aws.iam.state.IamRole;
import ca.vanzyl.ck8s.aws.iam.state.IamState;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.DeletePolicyParams;

public class DeletePolicyPreviewAction extends IamTaskAction<DeletePolicyParams> {

    private final IamState state;

    @Inject
    public DeletePolicyPreviewAction(IamClientFactory clientFactory, IamState state) {
        super(clientFactory);
        this.state = state;
    }

    @Override
    public Action action() {
        return Action.DELETE_POLICY;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.PREVIEW);
    }

    @Override
    public TaskResult execute(Context context, DeletePolicyParams input) throws Exception {
        var policyArn = input.policyArn();

        var policy = state.managedPolicy(input.baseParams(), policyArn);
        if (policy != null) {
            if (input.detachFromResources()) {
                var roles = state.listRolesForPolicy(input.baseParams(), policyArn);
                for (IamRole role : roles) {
                    var attachedPolicies = new HashSet<>(role.attachedPolicyArns());
                    attachedPolicies.remove(policyArn);
                    state.put(IamRole.builder().from(role)
                            .attachedPolicyArns(attachedPolicies)
                            .build());
                }
            }

            state.deleteManagedPolicy(policyArn);
        }
        return TaskResult.success();
    }
}
