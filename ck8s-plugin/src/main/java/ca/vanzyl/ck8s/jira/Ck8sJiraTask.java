package ca.vanzyl.ck8s.jira;

import ca.vanzyl.ck8s.actions.ActionUtils;
import ca.vanzyl.ck8s.actions.TaskActionExecutor;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@Named("ck8sJira")
@DryRunReady
public class Ck8sJiraTask implements Task {

    private final List<Ck8sJiraTaskAction<? extends Ck8sJiraTaskParams>> actions;
    private final Context context;

    @Inject
    public Ck8sJiraTask(List<Ck8sJiraTaskAction<?>> actions, Context context) {
        this.actions = ActionUtils.assertActions(actions);
        this.context = context;
    }

    @Override
    public TaskResult execute(Variables input) throws Exception {
        return TaskActionExecutor.execute(context, input, Ck8sJiraTaskAction.Action.class, actions, this::toActionInput);
    }

    private Ck8sJiraTaskParams toActionInput(Ck8sJiraTaskAction.Action action, Variables variables) {
        return switch (action) {
            case CREATE_VERSION, UPSERT_VERSION, UPDATE_VERSION -> VariablesCk8sJiraTaskParams.createVersion(context, variables);
            case EDIT_ISSUE -> VariablesCk8sJiraTaskParams.editIssue(context, variables);
            case GET_ISSUE -> VariablesCk8sJiraTaskParams.getIssue(context, variables);
            case GET_VERSION -> VariablesCk8sJiraTaskParams.getVersion(context, variables);
        };
    }
}
