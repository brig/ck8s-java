package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import ca.vanzyl.ck8s.aws.iam.state.IamState;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;

import javax.inject.Inject;
import java.util.Set;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.DeleteRoleParams;

public class DeleteRolePreviewAction extends IamTaskAction<DeleteRoleParams> {

    private final IamState state;

    @Inject
    public DeleteRolePreviewAction(IamClientFactory clientFactory, IamState state) {
        super(clientFactory);
        this.state = state;
    }

    @Override
    public Action action() {
        return Action.DELETE_ROLE;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.PREVIEW);
    }

    @Override
    public TaskResult execute(Context context, DeleteRoleParams input) throws Exception {
        var roleName = input.roleName();

        var stateRole = state.role(input.baseParams(), roleName);
        if (stateRole != null) {
            state.deleteRole(roleName);
        }

        return TaskResult.success();
    }
}
