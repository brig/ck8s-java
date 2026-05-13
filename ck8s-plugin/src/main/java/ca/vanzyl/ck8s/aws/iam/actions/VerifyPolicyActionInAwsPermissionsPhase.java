package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import ca.vanzyl.ck8s.cloudformation.CloudFormation;
import ca.vanzyl.ck8s.cloudformation.Resource;
import ca.vanzyl.ck8s.cloudformation.Statement;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;

import javax.inject.Inject;
import java.util.Set;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.CreatePolicyParams;

public class VerifyPolicyActionInAwsPermissionsPhase extends IamTaskAction<CreatePolicyParams> {

    private final CloudFormation cloudFormation;

    @Inject
    public VerifyPolicyActionInAwsPermissionsPhase(IamClientFactory clientFactory,
                                                   CloudFormation cloudFormation) {
        super(clientFactory);
        this.cloudFormation = cloudFormation;
    }

    @Override
    public Action action() {
        return Action.VERIFY_POLICY;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.AWS_PERMISSIONS);
    }

    @Override
    public TaskResult execute(Context context, CreatePolicyParams input) throws Exception {
        var policyName = input.policyName();

        cloudFormation.statement(new Statement(
                Statement.ALLOW,
                Set.of("iam:GetPolicy", "iam:GetPolicyVersion"),
                Set.of(Resource.sub("arn:aws:iam::${AWS::AccountId}:policy/" + policyName))));

        return TaskResult.success();
    }
}
