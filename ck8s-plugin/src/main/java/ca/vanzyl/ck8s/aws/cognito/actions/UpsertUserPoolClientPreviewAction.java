package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import ca.vanzyl.ck8s.aws.cognito.state.CognitoState;
import ca.vanzyl.ck8s.aws.cognito.state.UserPoolClient;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;

public class UpsertUserPoolClientPreviewAction extends CognitoTaskAction<CognitoTaskParams.CreateUserPoolClientParams> {

    private static final Logger log = LoggerFactory.getLogger(UpsertUserPoolClientPreviewAction.class);

    private final CognitoState state;

    @Inject
    public UpsertUserPoolClientPreviewAction(CognitoClientFactory clientFactory, CognitoState state) {
        super(clientFactory);
        this.state = state;
    }

    @Override
    public Action action() {
        return Action.UPSERT_USER_POOL_CLIENT;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.PREVIEW);
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateUserPoolClientParams input) throws Exception {
        var poolId = input.poolId();
        var clientName = input.name();

        var pool = state.userPoolById(input.baseParams(), poolId);
        if (pool == null) {
            log.info("[PREVIEW] Can't find user pool '{}' for client '{}'", poolId, clientName);
            return TaskResult.fail("User pool '" + poolId + "' does not exist.");
        }

        var userPoolClient = state.userPoolClient(input.baseParams(), poolId, clientName);
        if (userPoolClient == null) {
            log.info("[PREVIEW] User pool client '{}' in '{}' user pool does not exists. Creating it...", clientName, poolId);

            var clientId = "client_id_" + Math.abs(clientName.hashCode()); // TODO
            state.put(new UserPoolClient(poolId, clientId, clientName));
            return TaskResult.success()
                    .value("id", clientId);
        } else {
            log.info("[PREVIEW] User pool client '{}' exists in user pool '{}'. Updating it...", clientName, poolId);
            return TaskResult.success()
                    .value("id", userPoolClient.clientId());
        }
    }
}
