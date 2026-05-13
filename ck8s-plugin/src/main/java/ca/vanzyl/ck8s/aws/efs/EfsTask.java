package ca.vanzyl.ck8s.aws.efs;

import ca.vanzyl.ck8s.actions.ActionUtils;
import ca.vanzyl.ck8s.actions.TaskActionExecutor;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@Named("ck8sEfs")
@DryRunReady
public class EfsTask implements Task {

    private final List<EfsTaskAction<? extends EfsTaskParams>> actions;
    private final Context context;

    @Inject
    public EfsTask(List<EfsTaskAction<?>> actions, Context context) {
        this.actions = ActionUtils.assertActions(actions);
        this.context = context;
    }

    @Override
    public TaskResult execute(Variables input) throws Exception {
        return TaskActionExecutor.execute(context, input, EfsTaskAction.Action.class, actions, this::toActionInput);
    }

    private EfsTaskParams toActionInput(EfsTaskAction.Action action, Variables variables) {
        return switch (action) {
            case FIND_ACCESS_POINT -> VariablesEfsTaskParams.findAccessPointParams(context, variables);
            case FIND_FILE_SYSTEM -> VariablesEfsTaskParams.findFileSystemParams(context, variables);
            case CREATE_ACCESS_POINT -> VariablesEfsTaskParams.createAccessPointParams(context, variables);
            case DELETE_ACCESS_POINT -> VariablesEfsTaskParams.deleteAccessPointParams(context, variables);
        };
    }
}
