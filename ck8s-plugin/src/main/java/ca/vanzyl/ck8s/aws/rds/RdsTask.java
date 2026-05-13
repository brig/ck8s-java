package ca.vanzyl.ck8s.aws.rds;

import ca.vanzyl.ck8s.actions.ActionUtils;
import ca.vanzyl.ck8s.actions.TaskActionExecutor;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@Named("ck8sAwsRds")
@DryRunReady
public class RdsTask implements Task {

    private final List<RdsTaskAction<? extends RdsTaskParams>> actions;
    private final Context context;

    @Inject
    public RdsTask(List<RdsTaskAction<?>> actions, Context context) {
        this.actions = ActionUtils.assertActions(actions);
        this.context = context;
    }

    @Override
    public TaskResult execute(Variables input) throws Exception {
        return TaskActionExecutor.execute(context, input, RdsTaskAction.Action.class, actions, this::toActionInput);
    }

    private RdsTaskParams toActionInput(RdsTaskAction.Action action, Variables variables) {
        return switch (action) {
            case FETCH_ENDPOINT -> VariablesRdsTaskParams.createFetchEndpoint(context, variables);
        };
    }
}
