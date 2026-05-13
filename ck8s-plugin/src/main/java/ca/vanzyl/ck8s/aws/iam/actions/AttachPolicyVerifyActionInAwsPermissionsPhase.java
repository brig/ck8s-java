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

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.AttachPolicyParams;

public class AttachPolicyVerifyActionInAwsPermissionsPhase extends IamTaskAction<AttachPolicyParams> {

    private final CloudFormation cloudFormation;

    @Inject
    public AttachPolicyVerifyActionInAwsPermissionsPhase(IamClientFactory clientFactory,
                                                         CloudFormation cloudFormation) {
        super(clientFactory);
        this.cloudFormation = cloudFormation;
    }

    @Override
    public Action action() {
        return Action.ATTACH_POLICY_VERIFY;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.AWS_PERMISSIONS);
    }

    @Override
    public TaskResult execute(Context context, AttachPolicyParams input) throws Exception {
        cloudFormation.statement(new Statement(
                Statement.ALLOW,
                Set.of("iam:ListAttachedRolePolicies"),
                Set.of(Resource.sub("arn:aws:iam::${AWS::AccountId}:role/" + input.roleName()))));

        return TaskResult.success();
    }
}
