package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceNotFoundException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class UpsertUserPoolClientAction extends CognitoTaskAction<CognitoTaskParams.CreateUserPoolClientParams> {

    private static final Logger log = LoggerFactory.getLogger(UpsertUserPoolClientAction.class);

    @Inject
    public UpsertUserPoolClientAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.UPSERT_USER_POOL_CLIENT;
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateUserPoolClientParams input) throws Exception {
        var poolId = input.poolId();
        var clientName = input.name();
        var generateSecret = input.generateSecret();
        var refreshTokenValidity = input.refreshTokenValidity();
        var accessTokenValidity = input.accessTokenValidity();
        var idTokenValidity = input.idTokenValidity();
        var tokenValidityUnits = input.tokenValidityUnits();
        var supportedIdentityProviders = input.supportedIdentityProviders();
        var preventUserExistenceErrors = input.preventUserExistenceErrors();
        var enableTokenRevocation = input.enableTokenRevocation();
        var callbackUrls = input.callbackURLs();
        var logoutURLs = input.logoutURLs();
        var allowedOAuthFlowsUserPoolClient = input.allowedOAuthFlowsUserPoolClient();
        var allowedOAuthFlows = input.allowedOAuthFlows();
        var allowedOAuthScopes = input.allowedOAuthScopes();
        var explicitAuthFlows = input.explicitAuthFlows();

        try (var client = createClient(input)) {
            var existingClient = getClient(client, poolId, clientName);
            if (existingClient == null) {
                log.info("User pool client '{}' in '{}' user pool does not exists. Creating it...", clientName, poolId);

                var response = client.createUserPoolClient(r -> r.userPoolId(poolId)
                        .clientName(clientName)
                        .generateSecret(generateSecret)
                        .refreshTokenValidity(refreshTokenValidity)
                        .accessTokenValidity(accessTokenValidity)
                        .idTokenValidity(idTokenValidity)
                        .tokenValidityUnits(tokenValidityUnits)
                        .supportedIdentityProviders(supportedIdentityProviders)
                        .preventUserExistenceErrors(preventUserExistenceErrors)
                        .enableTokenRevocation(enableTokenRevocation)
                        .callbackURLs(callbackUrls)
                        .logoutURLs(logoutURLs)
                        .allowedOAuthFlowsUserPoolClient(allowedOAuthFlowsUserPoolClient)
                        .allowedOAuthFlows(allowedOAuthFlows)
                        .allowedOAuthScopes(allowedOAuthScopes)
                        .explicitAuthFlows(explicitAuthFlows)
                );

                log.info("✅ User pool client '{}' in pool '{}' created, id: '{}'", clientName, poolId, response.userPoolClient().clientId());

                return TaskResult.success()
                        .value("id", response.userPoolClient().clientId())
                        .value("secret", response.userPoolClient().clientSecret());
            } else {
                log.info("User pool client '{}' exists in user pool '{}'. Updating it...", clientName, poolId);

                var response = client.updateUserPoolClient(r -> r.userPoolId(poolId)
                        .clientId(existingClient.clientId())
                        .clientName(clientName)
                        .refreshTokenValidity(refreshTokenValidity)
                        .accessTokenValidity(accessTokenValidity)
                        .idTokenValidity(idTokenValidity)
                        .tokenValidityUnits(tokenValidityUnits)
                        .supportedIdentityProviders(supportedIdentityProviders)
                        .preventUserExistenceErrors(preventUserExistenceErrors)
                        .enableTokenRevocation(enableTokenRevocation)
                        .callbackURLs(merge(existingClient.callbackURLs(), callbackUrls))
                        .logoutURLs(merge(existingClient.logoutURLs(), logoutURLs))
                        .allowedOAuthFlowsUserPoolClient(allowedOAuthFlowsUserPoolClient)
                        .allowedOAuthFlows(allowedOAuthFlows)
                        .allowedOAuthScopes(allowedOAuthScopes)
                        .explicitAuthFlows(explicitAuthFlows)
                );

                log.info("✅ User pool client '{}' in user pool '{}' updated, id: '{}'", clientName, poolId, existingClient.clientId());

                return TaskResult.success()
                        .value("id", existingClient.clientId())
                        .value("secret", response.userPoolClient().clientSecret());
            }
        } catch (CognitoIdentityProviderException e) {
            log.error("❌ Failed to upsert user pool client '{}': {}", clientName, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }

    public static UserPoolClientType getClient(CognitoIdentityProviderClient client, String poolId, String clientName) {
        try {
            var clientDescription = FindUserPoolClientAction.findClientByName(client, poolId, clientName);
            if (clientDescription == null) {
                return null;
            }
            return client.describeUserPoolClient(r -> r.userPoolId(clientDescription.userPoolId())
                    .clientId(clientDescription.clientId())).userPoolClient();
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    public static UserPoolClientType getClientById(CognitoIdentityProviderClient client, String poolId, String clientId) {
        try {
            return client.describeUserPoolClient(r -> r.userPoolId(poolId)
                    .clientId(clientId)).userPoolClient();
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    public static Collection<String> merge(List<String> a, List<String> b) {
        var result = new HashSet<>(a);
        result.addAll(b);
        return result;
    }
}
