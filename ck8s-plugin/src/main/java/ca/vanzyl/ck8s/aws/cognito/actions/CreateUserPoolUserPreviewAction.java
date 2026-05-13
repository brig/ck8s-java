package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import ca.vanzyl.ck8s.aws.cognito.state.CognitoState;
import ca.vanzyl.ck8s.aws.cognito.state.UserPoolUser;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;

public class CreateUserPoolUserPreviewAction extends CognitoTaskAction<CognitoTaskParams.CreateUserPoolUserParams> {

    private static final Logger log = LoggerFactory.getLogger(CreateUserPoolUserPreviewAction.class);

    private final CognitoState state;

    @Inject
    public CreateUserPoolUserPreviewAction(CognitoClientFactory clientFactory, CognitoState state) {
        super(clientFactory);
        this.state = state;
    }

    @Override
    public Action action() {
        return Action.CREATE_USER_POOL_USER;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.PREVIEW);
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateUserPoolUserParams input) throws Exception {
        var poolId = input.poolId();
        var username = input.username();

        var existingUser = state.user(input.baseParams(), poolId, username);
        if (existingUser == null) {
            log.info("[PREVIEW] User '{}' in user pool '{}' does not exists. Creating it...", username, poolId);
            state.put(new UserPoolUser(poolId, username));
        } else {
            log.info("[PREVIEW] ]User '{}' in user pool '{}' exists. Do nothing...", username, poolId);
        }

        return TaskResult.success();
    }
}
