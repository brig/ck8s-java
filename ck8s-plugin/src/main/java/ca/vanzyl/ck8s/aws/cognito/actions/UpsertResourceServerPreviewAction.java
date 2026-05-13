package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import ca.vanzyl.ck8s.aws.cognito.state.CognitoState;
import ca.vanzyl.ck8s.aws.cognito.state.ResourceServer;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;

public class UpsertResourceServerPreviewAction extends CognitoTaskAction<CognitoTaskParams.CreateResourceServerParams> {

    private static final Logger log = LoggerFactory.getLogger(UpsertResourceServerPreviewAction.class);

    private final CognitoState state;

    @Inject
    public UpsertResourceServerPreviewAction(CognitoClientFactory clientFactory, CognitoState state) {
        super(clientFactory);
        this.state = state;
    }

    @Override
    public Action action() {
        return Action.UPSERT_RESOURCE_SERVER;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.PREVIEW);
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateResourceServerParams input) throws Exception {
        var poolId = input.poolId();
        var name = input.name();
        var identifier = input.identifier();

        var pool = state.userPoolById(input.baseParams(), poolId);
        if (pool == null) {
            log.info("[PREVIEW] Can't find user pool '{}' for resource server '{}'", poolId, name);
            return TaskResult.fail("User pool '" + poolId + "' does not exist.");
        }

        var existingResourceServer = state.resourceServer(input.baseParams(), poolId, identifier);
        if (existingResourceServer == null) {
            log.info("[PREVIEW] Resource server '{}' in pool '{}' does not exists. Creating it...", identifier, poolId);

            state.put(new ResourceServer(poolId, identifier, name));
            return TaskResult.success();
        } else {
            log.info("[PREVIEW] Resource server '{}' in pool '{}' exists. Updating it...", identifier, poolId);
            return TaskResult.success();
        }
    }
}
