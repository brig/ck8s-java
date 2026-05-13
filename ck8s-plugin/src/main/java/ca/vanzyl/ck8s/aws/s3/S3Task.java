package ca.vanzyl.ck8s.aws.s3;

import ca.vanzyl.ck8s.actions.ActionUtils;
import ca.vanzyl.ck8s.actions.TaskActionExecutor;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@Named("ck8sS3")
@DryRunReady
public class S3Task implements Task {

    private final List<S3TaskAction<? extends S3TaskParams>> actions;
    private final Context context;

    @Inject
    public S3Task(List<S3TaskAction<?>> actions, Context context) {
        this.actions = ActionUtils.assertActions(actions);
        this.context = context;
    }

    @Override
    public TaskResult execute(Variables input) throws Exception {
        return TaskActionExecutor.execute(context, input, S3TaskAction.Action.class, actions, this::toActionInput);
    }

    private S3TaskParams toActionInput(S3TaskAction.Action action, Variables variables) {
        return switch (action) {
            case CREATE_BUCKET, VERIFY_BUCKET -> VariablesS3TaskParams.createBucket(context, variables);
            case TAG_BUCKET, VERIFY_BUCKET_TAGS, TAG_BUCKET_DEPRECATED_ACTION -> VariablesS3TaskParams.tagBucket(context, variables);
        };
    }
}
