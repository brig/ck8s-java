package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

import javax.inject.Inject;
import java.util.Set;

public class VerifyUserPoolUserAction extends CognitoTaskAction<CognitoTaskParams.CreateUserPoolUserParams> {

    private static final Logger log = LoggerFactory.getLogger(VerifyUserPoolUserAction.class);

    @Inject
    public VerifyUserPoolUserAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.VERIFY_USER_POOL_USER;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateUserPoolUserParams input) throws Exception {
        var poolId = input.poolId();
        var username = input.username();

        try (var client = createClient(input)) {
            var existingUser = CreateUserPoolUserAction.getUser(client, poolId, username);
            if (existingUser == null) {
                log.error("❌ User '{}' does not exists in user pool '{}'", username, poolId);
                return TaskResult.fail("User not found in user pool");
            }

            log.info("✅ User '{}' exists in pool '{}'", username, poolId);
            return TaskResult.success();
        } catch (CognitoIdentityProviderException e) {
            log.error("❌ Failed to verify user {} in pool '{}': {}", username, poolId, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }
}
