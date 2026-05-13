package ca.vanzyl.ck8s.aws.glue.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.glue.GlueClientFactory;
import ca.vanzyl.ck8s.aws.glue.GlueTaskAction;
import ca.vanzyl.ck8s.aws.glue.GlueTaskParams;
import ca.vanzyl.ck8s.cloudformation.CloudFormation;
import ca.vanzyl.ck8s.cloudformation.Resource;
import ca.vanzyl.ck8s.cloudformation.Statement;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;

import javax.inject.Inject;
import java.util.Set;

public class ExistsDatabaseActionInAwsPermissionsPhase extends GlueTaskAction<GlueTaskParams.ExistsParams> {

    private final CloudFormation cloudFormation;

    @Inject
    public ExistsDatabaseActionInAwsPermissionsPhase(GlueClientFactory clientFactory,
                                                     CloudFormation cloudFormation) {
        super(clientFactory);
        this.cloudFormation = cloudFormation;
    }

    @Override
    public Action action() {
        return Action.EXISTS;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.AWS_PERMISSIONS);
    }

    @Override
    public TaskResult execute(Context context, GlueTaskParams.ExistsParams input) throws Exception {
        var name = input.databaseName();

        cloudFormation.statement(new Statement(
                Statement.ALLOW,
                Set.of("glue:GetDatabase"),
                Set.of(Resource.sub("arn:aws:glue:${AWS::Region}:${AWS::AccountId}:database/" + name))));
        return TaskResult.success();
    }
}
