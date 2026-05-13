package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import ca.vanzyl.ck8s.aws.iam.state.IamManagedPolicy;
import ca.vanzyl.ck8s.aws.iam.state.IamState;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;

import javax.inject.Inject;
import java.util.Set;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.CreatePolicyParams;
import static ca.vanzyl.ck8s.aws.iam.actions.CreatePolicyAction.dumpInput;

public class CreatePolicyOrVerifyPreviewAction extends IamTaskAction<CreatePolicyParams> {

    private final IamState state;

    @Inject
    public CreatePolicyOrVerifyPreviewAction(IamClientFactory clientFactory, IamState state) {
        super(clientFactory);
        this.state = state;
    }

    @Override
    public Action action() {
        return Action.CREATE_POLICY_OR_VERIFY;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.PREVIEW);
    }

    @Override
    public TaskResult execute(Context context, CreatePolicyParams input) throws Exception {
        var policyName = input.policyName();
        var policyArn = input.policyArn();
        var policyDocument = input.policyDocument();

        dumpInput(input);

        var statePolicy = state.managedPolicy(input.baseParams(), policyArn);
        if (statePolicy != null) {
            var currentDocument = statePolicy.document();
            return CreatePolicyOrVerifyAction.diffPolicy(currentDocument, policyDocument);
        }
        state.put(new IamManagedPolicy(policyArn, policyName, policyDocument));

        return TaskResult.success();
    }
}
