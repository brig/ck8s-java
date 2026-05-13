package ca.vanzyl.ck8s.aws.asm;

import ca.vanzyl.ck8s.actions.ActionUtils;
import ca.vanzyl.ck8s.actions.TaskActionExecutor;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@Named("ck8sAwsAsm")
@DryRunReady
public class AsmTask implements Task {

    private final List<AsmTaskAction<? extends AsmTaskParams>> actions;
    private final Context context;

    @Inject
    public AsmTask(List<AsmTaskAction<?>> actions, Context context) {
        this.actions = ActionUtils.assertActions(actions);
        this.context = context;
    }

    @Override
    @SensitiveData(keys = {"secretString", "secretValue"})
    public TaskResult execute(Variables input) throws Exception {
        return TaskActionExecutor.execute(context, input, AsmTaskAction.Action.class, actions, this::toActionInput);
    }

    private AsmTaskParams toActionInput(AsmTaskAction.Action action, Variables variables) {
        return switch (action) {
            case CREATE_SECRET -> VariablesAsmTaskParams.createSecret(variables);
            case GET_SECRET -> VariablesAsmTaskParams.getSecret(variables);
            case UPDATE_SECRET -> VariablesAsmTaskParams.updateSecret(variables);
            case DELETE_SECRET -> VariablesAsmTaskParams.deleteSecret(variables);
        };
    }
}