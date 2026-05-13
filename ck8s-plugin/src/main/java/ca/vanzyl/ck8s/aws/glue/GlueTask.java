package ca.vanzyl.ck8s.aws.glue;

import ca.vanzyl.ck8s.actions.ActionUtils;
import ca.vanzyl.ck8s.actions.TaskActionExecutor;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@Named("ck8sGlue")
@DryRunReady
public class GlueTask implements Task {

    private final List<GlueTaskAction<? extends GlueTaskParams>> actions;
    private final Context context;

    @Inject
    public GlueTask(List<GlueTaskAction<?>> actions, Context context) {
        this.actions = ActionUtils.assertActions(actions);
        this.context = context;
    }

    @Override
    public TaskResult execute(Variables input) throws Exception {
        return TaskActionExecutor.execute(context, input, GlueTaskAction.Action.class, actions, this::toActionInput);
    }

    private GlueTaskParams toActionInput(GlueTaskAction.Action action, Variables variables) {
        return switch (action) {
            case EXISTS -> VariablesGlueTaskParams.exists(context, variables);
        };
    }
}
