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
import java.util.ArrayList;
import java.util.HashSet;

import static ca.vanzyl.ck8s.aws.cognito.actions.UpsertUserPoolClientAction.getClientById;

public class UpsertUserPoolClientCallbacksAction extends CognitoTaskAction<CognitoTaskParams.UserPoolClientCallbacks> {

    private static final Logger log = LoggerFactory.getLogger(UpsertUserPoolClientCallbacksAction.class);

    @Inject
    public UpsertUserPoolClientCallbacksAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.UPSERT_USER_POOL_CLIENT_CALLBACKS;
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

            var resultCallbackUrls = new ArrayList<>(existingClient.callbackURLs());
            var callbackUrls = new HashSet<>(existingClient.callbackURLs());

            var resultLogoutUrls = new ArrayList<>(existingClient.logoutURLs());
            var logoutUrs = new HashSet<>(existingClient.logoutURLs());

            for (var url : input.urls()) {
                if (!callbackUrls.contains(url)) {
                    log.info("Adding '{}' to the callback URLs", url);
                    resultCallbackUrls.add(url);
                } else {
                    log.info("Callback URL '{}' already exists", url);
                }
                if (!logoutUrs.contains(url)) {
                    log.info("Adding '{}' to the logout URLs", url);
                    resultLogoutUrls.add(url);
                } else {
                    log.info("Logout URL '{}' already exists", url);
                }
            }

            var callbacksModified = resultCallbackUrls.size() != existingClient.callbackURLs().size();
            var logoutModified = resultLogoutUrls.size() != existingClient.logoutURLs().size();

            if (!callbacksModified && !logoutModified) {
                log.info("✅ User pool client '{}' in pool '{}' contains all urls '{}', do nothing", clientId, poolId, input.urls());
                return TaskResult.success();
            }

            var response = client.updateUserPoolClient(r -> r.userPoolId(poolId)
                    .clientId(existingClient.clientId())
                    .clientName(existingClient.clientName())
                    .refreshTokenValidity(existingClient.refreshTokenValidity())
                    .accessTokenValidity(existingClient.accessTokenValidity())
                    .idTokenValidity(existingClient.idTokenValidity())
                    .tokenValidityUnits(existingClient.tokenValidityUnits())
                    .supportedIdentityProviders(existingClient.supportedIdentityProviders())
                    .preventUserExistenceErrors(existingClient.preventUserExistenceErrors())
                    .enableTokenRevocation(existingClient.enableTokenRevocation())
                    .callbackURLs(callbacksModified ? resultCallbackUrls: existingClient.callbackURLs())
                    .logoutURLs(logoutModified ? resultLogoutUrls : existingClient.logoutURLs())
                    .allowedOAuthFlowsUserPoolClient(existingClient.allowedOAuthFlowsUserPoolClient())
                    .allowedOAuthFlows(existingClient.allowedOAuthFlows())
                    .explicitAuthFlows(existingClient.explicitAuthFlows())
            );

            log.info("✅ User pool client '{}' in pool '{}' updated, id: '{}'", clientId, poolId, response.userPoolClient().clientId());

            return TaskResult.success();
        } catch (CognitoIdentityProviderException e) {
            log.error("❌ Failed to upsert user pool client '{}' callback urls: {}", clientId, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }
}
