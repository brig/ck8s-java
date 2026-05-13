package ca.vanzyl.ck8s.aws.cloudformation.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationClientFactory;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTaskAction;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTaskParams;
import ca.vanzyl.ck8s.preview.PreviewChangesRecorder;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;

public class DeployPreviewAction extends CloudFormationTaskAction<CloudFormationTaskParams.DeployParams> {

    private static final Logger log = LoggerFactory.getLogger(DeployPreviewAction.class);

    private static final String TYPE = "aws:cloudformation:Stack";

    private final PreviewChangesRecorder preview;

    @Inject
    public DeployPreviewAction(CloudFormationClientFactory clientFactory, PreviewChangesRecorder preview) {
        super(clientFactory);
        this.preview = preview;
    }

    @Override
    public Action action() {
        return Action.DEPLOY;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.PREVIEW);
    }

    @Override
    public TaskResult execute(Context context, CloudFormationTaskParams.DeployParams input) {
        return TaskResult.success();
    }
}
