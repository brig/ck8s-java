package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import ca.vanzyl.ck8s.aws.cognito.state.CognitoState;
import ca.vanzyl.ck8s.aws.cognito.state.UserPool;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;

public class UpsertUserPoolPreviewAction extends CognitoTaskAction<CognitoTaskParams.CreateUserPoolParams> {

    private static final Logger log = LoggerFactory.getLogger(UpsertUserPoolPreviewAction.class);

    private final CognitoState state;

    @Inject
    public UpsertUserPoolPreviewAction(CognitoClientFactory clientFactory, CognitoState state) {
        super(clientFactory);
        this.state = state;
    }

    @Override
    public Action action() {
        return Action.UPSERT_USER_POOL;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.PREVIEW);
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateUserPoolParams input) throws Exception {
        var name = input.poolName();

        var poolId = "pool_id_" + Math.abs(name.hashCode()); // TODO
        var pool = state.userPoolByName(input.baseParams(), name, poolId);
        if (pool == null) {
            log.info("[PREVIEW] Pool '{}' does not exists. Creating it...", name);
            state.put(new UserPool(poolId, name));
            return TaskResult.success()
                    .value("id", poolId);
        } else {
            log.info("[PREVIEW] Pool '{}' exists. Updating it...", name);
            return TaskResult.success()
                    .value("id", pool.id());
        }
    }
}
