package ca.vanzyl.ck8s.aws.cognito;

import ca.vanzyl.ck8s.actions.ActionInput;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.List;
import java.util.Map;

public interface CognitoTaskParams extends ActionInput {

    BaseParams baseParams();

    record BaseParams(String profile, Region region) {
    }

    record GetUserPoolClientParams(
            BaseParams baseParams,
            String poolId,
            String clientId
    ) implements CognitoTaskParams {
    }

    record ListUserPoolsParams(
            BaseParams baseParams,
            int maxResults
    ) implements CognitoTaskParams {
    }

    record FindUserPoolParams(
            BaseParams baseParams,
            String poolName
    ) implements CognitoTaskParams {
    }

    record FindUserPoolClientParams(
            BaseParams baseParams,
            String poolId,
            String clientName
    ) implements CognitoTaskParams {
    }

    record CreateUserPoolParams(
            BaseParams baseParams,
            String poolName,
            List<UsernameAttributeType> usernameAttributes,
            UsernameConfigurationType usernameConfiguration,
            UserPoolPolicyType policy,
            AdminCreateUserConfigType adminCreateUserConfig,
            EmailConfigurationType emailConfiguration,
            List<SchemaAttributeType> schema,
            Map<String, String> tags
    ) implements CognitoTaskParams {
    }

    record CreateUserPoolDomainParams(
            BaseParams baseParams,
            String poolId,
            String domain
    ) implements CognitoTaskParams {
    }

    record CreateUserPoolUserParams(
            BaseParams baseParams,
            String poolId,
            String username,
            String password
    ) implements CognitoTaskParams {
    }

    record CreateUserPoolUICustomizationParams(
            BaseParams baseParams,
            String poolId,
            byte[] image,
            String css
    ) implements CognitoTaskParams {
    }

    record DeleteUserPoolsParams(BaseParams baseParams, List<String> ids) implements CognitoTaskParams {
    }

    record DeleteUserPoolParams(BaseParams baseParams, String poolName, int maxResults) implements CognitoTaskParams {
    }

    record CreateIdentityProviderParams(
            BaseParams baseParams,
            String poolId,
            String providerName,
            String providerType,
            Map<String, String> providerDetails,
            Map<String, String> attributeMapping
    ) implements CognitoTaskParams {
    }

    record CreateResourceServerParams(
            BaseParams baseParams,
            String poolId,
            String name,
            String identifier,
            List<ResourceServerScopeType> scopes
    ) implements CognitoTaskParams {
    }

    record CreateUserPoolClientParams(
            BaseParams baseParams,
            String poolId,
            String name,
            boolean generateSecret,
            Integer refreshTokenValidity,
            Integer accessTokenValidity,
            Integer idTokenValidity,
            TokenValidityUnitsType tokenValidityUnits,
            List<String> supportedIdentityProviders,
            PreventUserExistenceErrorTypes preventUserExistenceErrors,
            boolean enableTokenRevocation,
            List<String> callbackURLs,
            List<String> logoutURLs,
            boolean allowedOAuthFlowsUserPoolClient,
            List<OAuthFlowType> allowedOAuthFlows,
            List<String> allowedOAuthScopes,
            List<ExplicitAuthFlowsType> explicitAuthFlows
    ) implements CognitoTaskParams {
    }

    record UserPoolClientCallbacks(
            BaseParams baseParams,
            String poolId,
            String clientId,
            List<String> urls
    ) implements CognitoTaskParams {
    }
}
