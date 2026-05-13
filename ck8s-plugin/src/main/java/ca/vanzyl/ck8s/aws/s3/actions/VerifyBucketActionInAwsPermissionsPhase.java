package ca.vanzyl.ck8s.aws.s3.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.s3.S3ClientFactory;
import ca.vanzyl.ck8s.aws.s3.S3TaskAction;
import ca.vanzyl.ck8s.cloudformation.CloudFormation;
import ca.vanzyl.ck8s.cloudformation.Resource;
import ca.vanzyl.ck8s.cloudformation.Statement;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;

import javax.inject.Inject;
import java.util.Set;

import static ca.vanzyl.ck8s.aws.s3.S3TaskParams.CreateBucketParams;

public class VerifyBucketActionInAwsPermissionsPhase extends S3TaskAction<CreateBucketParams> {

    private final CloudFormation cloudFormation;

    @Inject
    public VerifyBucketActionInAwsPermissionsPhase(S3ClientFactory clientFactory, CloudFormation cloudFormation) {
        super(clientFactory);
        this.cloudFormation = cloudFormation;
    }

    @Override
    public Action action() {
        return Action.VERIFY_BUCKET;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.AWS_PERMISSIONS);
    }

    @Override
    public TaskResult execute(Context context, CreateBucketParams input) throws Exception {
        var bucket = input.bucket();

        cloudFormation.statement(new Statement(
                Statement.ALLOW,
                Set.of("s3:GetBucketAcl"),
                Set.of(Resource.sub("arn:aws:s3::" + bucket))));

        return TaskResult.success();
    }
}
