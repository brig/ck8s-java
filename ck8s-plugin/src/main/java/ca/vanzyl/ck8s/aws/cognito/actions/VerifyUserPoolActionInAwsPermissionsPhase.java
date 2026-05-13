package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import ca.vanzyl.ck8s.cloudformation.CloudFormation;
import ca.vanzyl.ck8s.cloudformation.Resource;
import ca.vanzyl.ck8s.cloudformation.Statement;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;

import javax.inject.Inject;
import java.util.Set;

public class VerifyUserPoolActionInAwsPermissionsPhase extends CognitoTaskAction<CognitoTaskParams.CreateUserPoolParams> {

    private final CloudFormation cloudFormation;

    @Inject
    public VerifyUserPoolActionInAwsPermissionsPhase(CognitoClientFactory clientFactory, CloudFormation cloudFormation) {
        super(clientFactory);
        this.cloudFormation = cloudFormation;
    }

    @Override
    public Action action() {
        return Action.VERIFY_USER_POOL;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.AWS_PERMISSIONS);
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateUserPoolParams input) throws Exception {
        cloudFormation.statement(new Statement(
                Statement.ALLOW,
                Set.of("cognito-idp:ListUserPools"),
                Set.of(Resource.plain("*"))));

        cloudFormation.statement(new Statement(
                Statement.ALLOW,
                Set.of("cognito-idp:DescribeUserPool"),
                Set.of(Resource.sub("arn:aws:cognito-idp:${AWS::Region}:${AWS::AccountId}:userpool/#{CognitoUserPoolId}"))));

        return TaskResult.success()
                .value("id", "#{CognitoUserPoolId}");
    }
}
