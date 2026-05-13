package ca.vanzyl.ck8s.aws.iam;

import ca.vanzyl.ck8s.actions.ActionUtils;
import ca.vanzyl.ck8s.actions.TaskActionExecutor;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@Named("ck8sIam")
@DryRunReady
public class IamTask implements Task {

    private final List<IamTaskAction<? extends IamTaskParams>> actions;
    private final Context context;

    @Inject
    public IamTask(List<IamTaskAction<?>> actions, Context context) {
        this.actions = ActionUtils.assertActions(actions);
        this.context = context;
    }

    @Override
    public TaskResult execute(Variables input) throws Exception {
        return TaskActionExecutor.execute(context, input, IamTaskAction.Action.class, actions, this::toActionInput);
    }

    private IamTaskParams toActionInput(IamTaskAction.Action action, Variables variables) {
        return switch (action) {
            case CREATE_ROLE, CREATE_ROLE_OR_VERIFY, VERIFY_ROLE -> VariablesIamTaskParams.createRole(context, variables);
            case DELETE_ROLE -> VariablesIamTaskParams.deleteRole(context, variables);
            case GET_ROLE -> VariablesIamTaskParams.getRole(context, variables);
            case LIST_ROLES -> VariablesIamTaskParams.listRoles(context, variables);
            case PUT_ROLE_POLICY, PUT_ROLE_POLICY_OR_VERIFY, VERIFY_INLINE_POLICY -> VariablesIamTaskParams.putRolePolicy(context, variables);
            case CREATE_POLICY, CREATE_POLICY_OR_VERIFY, VERIFY_POLICY -> VariablesIamTaskParams.createPolicy(context, variables);
            case DELETE_POLICY -> VariablesIamTaskParams.deletePolicy(context, variables);
            case LIST_POLICIES -> VariablesIamTaskParams.listPolicies(context, variables);
            case ATTACH_POLICY, ATTACH_POLICY_VERIFY -> VariablesIamTaskParams.attachPolicy(context, variables);
        };
    }
}
