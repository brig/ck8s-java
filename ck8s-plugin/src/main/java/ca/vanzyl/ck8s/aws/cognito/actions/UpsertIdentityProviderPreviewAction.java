package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import ca.vanzyl.ck8s.aws.cognito.state.CognitoState;
import ca.vanzyl.ck8s.aws.cognito.state.IdentityProvider;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;

public class UpsertIdentityProviderPreviewAction extends CognitoTaskAction<CognitoTaskParams.CreateIdentityProviderParams> {

    private static final Logger log = LoggerFactory.getLogger(UpsertIdentityProviderPreviewAction.class);

    private final CognitoState state;

    @Inject
    public UpsertIdentityProviderPreviewAction(CognitoClientFactory clientFactory, CognitoState state) {
        super(clientFactory);
        this.state = state;
    }

    @Override
    public Action action() {
        return Action.UPSERT_IDENTITY_PROVIDER;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.PREVIEW);
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateIdentityProviderParams input) throws Exception {
        var poolId = input.poolId();
        var name = input.providerName();

        var pool = state.userPoolById(input.baseParams(), poolId);
        if (pool == null) {
            log.info("[PREVIEW] Can't find user pool '{}' for identity provider '{}'", poolId, name);
            return TaskResult.fail("User pool '" + poolId + "' does not exist.");
        }

        var identityProvider = state.identityProvider(input.baseParams(), poolId, name);
        if (identityProvider == null) {
            log.info("[PREVIEW] Identity provider '{}' does not exists. Creating it...", name);
            state.put(new IdentityProvider(poolId, name));
        } else {
            log.info("[PREVIEW] Identity provider '{}' exists. Updating it...", name);
            // TODO
        }

        return TaskResult.success();
    }
}
