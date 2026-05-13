package ca.vanzyl.ck8s.aws.cloudformation.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationClientFactory;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTaskAction;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTaskParams;
import ca.vanzyl.ck8s.aws.cloudformation.state.CloudFormationState;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;

import javax.inject.Inject;
import java.util.Set;

public class DeletePreviewAction extends CloudFormationTaskAction<CloudFormationTaskParams.DeleteParams> {

    private final CloudFormationState state;

    @Inject
    public DeletePreviewAction(CloudFormationClientFactory clientFactory, CloudFormationState state) {
        super(clientFactory);
        this.state = state;
    }

    @Override
    public Action action() {
        return Action.DELETE;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.PREVIEW);
    }

    @Override
    public TaskResult execute(Context context, CloudFormationTaskParams.DeleteParams input) {
        var stackName = input.stackName();

        var stack = state.stack(input.baseParams(), stackName);
        if (stack != null) {
            state.deleteStack(input.baseParams().region().id(), stack.stackName());
        }

        return TaskResult.success();
    }
}
