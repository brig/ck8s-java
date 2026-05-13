package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

import static ca.vanzyl.ck8s.aws.cognito.actions.UpsertUserPoolClientAction.merge;
import static ca.vanzyl.ck8s.utils.VerifyUtils.*;

public class VerifyUserPoolClientAction extends CognitoTaskAction<CognitoTaskParams.CreateUserPoolClientParams> {

    private static final Logger log = LoggerFactory.getLogger(VerifyUserPoolClientAction.class);

    @Inject
    public VerifyUserPoolClientAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.VERIFY_USER_POOL_CLIENT;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateUserPoolClientParams input) throws Exception {
        var poolId = input.poolId();
        var clientName = input.name();

        try (var client = createClient(input)) {
            var existingClient = UpsertUserPoolClientAction.getClient(client, poolId, clientName);
            if (existingClient == null) {
                log.error("❌ User Pool client '{}' does not exists in user pool '{}'", clientName, poolId);
                return TaskResult.fail("Cognito User pool client not found");
            }

            log.info("User pool client '{}' exists in user pool '{}'. Verifying it...", clientName, poolId);

            return diffUserPoolClient(existingClient, UserPoolClientType.builder()
                    .userPoolId(poolId)
                    .clientName(clientName)
                    .refreshTokenValidity(input.refreshTokenValidity())
                    .accessTokenValidity(input.accessTokenValidity())
                    .idTokenValidity(input.idTokenValidity())
                    .tokenValidityUnits(input.tokenValidityUnits())
                    .supportedIdentityProviders(input.supportedIdentityProviders())
                    .preventUserExistenceErrors(input.preventUserExistenceErrors())
                    .enableTokenRevocation(input.enableTokenRevocation())
                    .callbackURLs(merge(existingClient.callbackURLs(), input.callbackURLs()))
                    .logoutURLs(merge(existingClient.logoutURLs(), input.logoutURLs()))
                    .allowedOAuthFlowsUserPoolClient(input.allowedOAuthFlowsUserPoolClient())
                    .allowedOAuthFlows(input.allowedOAuthFlows())
                    .allowedOAuthScopes(input.allowedOAuthScopes())
                    .explicitAuthFlows(input.explicitAuthFlows())
                    .build());
        }
    }

    private TaskResult diffUserPoolClient(UserPoolClientType existingClient, UserPoolClientType newClient) {
        var valid = true;

        valid &= verifyAttribute("Name", existingClient.clientName(), newClient.clientName());
        valid &= verifyAttribute("Refresh token validity", existingClient.refreshTokenValidity(), newClient.refreshTokenValidity());
        valid &= verifyAttribute("Access token validity", existingClient.accessTokenValidity(), newClient.accessTokenValidity());
        valid &= verifyAttribute("ID token validity", existingClient.idTokenValidity(), newClient.idTokenValidity());
        valid &= verifyAttribute("Token validity Units", existingClient.tokenValidityUnits(), newClient.tokenValidityUnits());
        valid &= verifyAttribute("Supported identity providers", existingClient.supportedIdentityProviders(), newClient.supportedIdentityProviders());
        valid &= verifyAttribute("Prevent user existence errors", existingClient.preventUserExistenceErrors(), newClient.preventUserExistenceErrors());
        valid &= verifyAttribute("Enable token revocation", existingClient.enableTokenRevocation(), newClient.enableTokenRevocation());
        valid &= verifyPartialListMatch("Callback URLs", existingClient.callbackURLs(), newClient.callbackURLs());
        valid &= verifyPartialListMatch("Logout URLs", existingClient.logoutURLs(), newClient.logoutURLs());
        valid &= verifyAttribute("Allowed OAuth Flows UserPoolClient", existingClient.allowedOAuthFlowsUserPoolClient(), newClient.allowedOAuthFlowsUserPoolClient());
        valid &= verifyAttribute("Allowed OAuth Flows", existingClient.allowedOAuthFlows(), newClient.allowedOAuthFlows());
        valid &= verifyAttribute("Allowed OAuth Scopes", existingClient.allowedOAuthScopes(), newClient.allowedOAuthScopes());
        valid &= verifyPartialListMatch("Explicit Auth Flows", existingClient.explicitAuthFlows(), newClient.explicitAuthFlows());

        if (!valid) {
            log.info("❌ User pool client '{}' changed", existingClient.clientName());

            var attrs = List.of("userPoolId", "clientName", "clientId", "refreshTokenValidity", "accessTokenValidity", "idTokenValidity", "tokenValidityUnits", "explicitAuthFlows",
                    "supportedIdentityProviders", "callbackURLs", "logoutURLs", "allowedOAuthFlows", "allowedOAuthFlowsUserPoolClient", "allowedOAuthScopes", "preventUserExistenceErrors");
            dumpDiff("user pool client", existingClient, newClient, attrs);

            return TaskResult.fail("User pool client changed");
        }

        log.info("✅ User pool client '{}' unchanged", existingClient.clientName());

        return TaskResult.success()
                .value("id", existingClient.clientId())
                .value("secret", existingClient.clientSecret());
    }
}
