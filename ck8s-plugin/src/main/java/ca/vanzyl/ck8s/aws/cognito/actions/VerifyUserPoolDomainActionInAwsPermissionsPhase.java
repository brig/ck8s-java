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

public class VerifyUserPoolDomainActionInAwsPermissionsPhase extends CognitoTaskAction<CognitoTaskParams.CreateUserPoolDomainParams> {

    private final CloudFormation cloudFormation;

    @Inject
    public VerifyUserPoolDomainActionInAwsPermissionsPhase(CognitoClientFactory clientFactory, CloudFormation cloudFormation) {
        super(clientFactory);
        this.cloudFormation = cloudFormation;
    }

    @Override
    public Action action() {
        return Action.VERIFY_USER_POOL_DOMAIN;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.AWS_PERMISSIONS);
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateUserPoolDomainParams input) throws Exception {
        cloudFormation.statement(new Statement(
                Statement.ALLOW,
                Set.of("cognito-idp:DescribeUserPoolDomain"),
                Set.of(Resource.plain("*"))));

        return TaskResult.success();
    }
}
