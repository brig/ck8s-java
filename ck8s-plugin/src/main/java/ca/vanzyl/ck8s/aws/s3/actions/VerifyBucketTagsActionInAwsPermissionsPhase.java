package ca.vanzyl.ck8s.aws.s3.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.s3.S3ClientFactory;
import ca.vanzyl.ck8s.aws.s3.S3TaskAction;
import ca.vanzyl.ck8s.aws.s3.S3TaskParams;
import ca.vanzyl.ck8s.cloudformation.CloudFormation;
import ca.vanzyl.ck8s.cloudformation.Resource;
import ca.vanzyl.ck8s.cloudformation.Statement;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;

import javax.inject.Inject;
import java.util.Set;

public class VerifyBucketTagsActionInAwsPermissionsPhase extends S3TaskAction<S3TaskParams.TagBucketParams> {

    private final CloudFormation cloudFormation;

    @Inject
    public VerifyBucketTagsActionInAwsPermissionsPhase(S3ClientFactory clientFactory,
                                                       CloudFormation cloudFormation) {
        super(clientFactory);
        this.cloudFormation = cloudFormation;
    }

    @Override
    public Action action() {
        return Action.VERIFY_BUCKET_TAGS;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.AWS_PERMISSIONS);
    }

    @Override
    public TaskResult execute(Context context, S3TaskParams.TagBucketParams input) throws Exception {
        var bucket = input.bucket();

        cloudFormation.statement(new Statement(
                Statement.ALLOW,
                Set.of("s3:GetBucketAcl", "s3:GetBucketTagging"),
                Set.of(Resource.sub("arn:aws:s3::" + bucket))));

        return TaskResult.success();
    }
}
