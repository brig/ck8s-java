package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.google.inject.Inject;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

import java.util.Map;
import java.util.Set;

public class FindUserPoolAction extends CognitoTaskAction<CognitoTaskParams.FindUserPoolParams> {

    private static final Logger log = LoggerFactory.getLogger(FindUserPoolAction.class);

    @Inject
    public FindUserPoolAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.FIND_USER_POOL;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.FindUserPoolParams input) throws Exception {
        var poolName = input.poolName();
        try (var client = createClient(input)) {
            var userPoolId = UpsertUserPoolAction.finPoolIdByName(client, poolName);

            if (userPoolId == null) {
                log.info("No user pools found for name: '{}'", poolName);
                return TaskResult.success();
            }

            return TaskResult.success()
                    .value("pool", Map.of("id", userPoolId));
        } catch (CognitoIdentityProviderException e) {
            log.error("❌ Failed to find user pool '{}': {}", poolName, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }
}
