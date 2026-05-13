package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

import javax.inject.Inject;
import java.util.HashSet;

import static ca.vanzyl.ck8s.aws.cognito.actions.UpsertUserPoolClientAction.getClientById;

public class VerifyUserPoolClientCallbacksAction extends CognitoTaskAction<CognitoTaskParams.UserPoolClientCallbacks> {

    private static final Logger log = LoggerFactory.getLogger(VerifyUserPoolClientCallbacksAction.class);

    @Inject
    public VerifyUserPoolClientCallbacksAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.VERIFY_USER_POOL_CLIENT_CALLBACKS;
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.UserPoolClientCallbacks input) throws Exception {
        var poolId = input.poolId();
        var clientId = input.clientId();

        try (var client = createClient(input)) {
            var existingClient = getClientById(client, poolId, clientId);
            if (existingClient == null) {
                log.info("❌ User pool client '{}' in '{}' user pool does not exists", clientId, poolId);
                return TaskResult.fail("User pool client does not exists");
            }

            var callbackUrls = new HashSet<>(existingClient.callbackURLs());

            var logoutUrs = new HashSet<>(existingClient.logoutURLs());

            var success = true;
            for (var url : input.urls()) {
                if (callbackUrls.contains(url)) {
                    log.info("✅ Callback URL '{}' exists", url);
                } else {
                    log.error("❌ Callback URL '{}' does not exists", url);
                    success = false;
                }
                if (logoutUrs.contains(url)) {
                    log.info("✅ Logout URL '{}' exists", url);
                } else {
                    log.error("❌ Logout URL '{}' does not exists", url);
                    success = false;
                }
            }

            if (success) {
                return TaskResult.success();
            } else {
                return TaskResult.fail("Missing callback/logout URLs");
            }
        } catch (CognitoIdentityProviderException e) {
            log.error("❌ Failed to verify user pool client '{}' callback urls: {}", clientId, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }
}
