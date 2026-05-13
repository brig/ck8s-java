package ca.vanzyl.ck8s.github;

import ca.vanzyl.ck8s.actions.ActionUtils;
import ca.vanzyl.ck8s.actions.TaskActionExecutor;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@Named("ck8sGithub")
@DryRunReady
public class Ck8sGithubTask implements Task {

    private final List<Ck8sGithubTaskAction<? extends Ck8sGithubTaskParams>> actions;
    private final Context context;

    @Inject
    public Ck8sGithubTask(List<Ck8sGithubTaskAction<?>> actions, Context context) {
        this.actions = ActionUtils.assertActions(actions);
        this.context = context;
    }

    @Override
    public TaskResult execute(Variables input) throws Exception {
        return TaskActionExecutor.execute(context, input, Ck8sGithubTaskAction.Action.class, actions, this::toActionInput);
    }

    private Ck8sGithubTaskParams toActionInput(Ck8sGithubTaskAction.Action action, Variables variables) {
        return switch (action) {
            case LIST_COMMITS -> VariablesCk8sGithubTaskParams.listCommits(context, variables);
            case GET_SHORT_SHA -> VariablesCk8sGithubTaskParams.getShortSha(context, variables);
        };
    }
}
