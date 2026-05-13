package ca.vanzyl.ck8s.aws.cognito;

import ca.vanzyl.ck8s.aws.AwsTaskUtils;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams.*;
import ca.vanzyl.ck8s.common.MapUtils;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import software.amazon.awssdk.utils.builder.SdkBuilder;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ca.vanzyl.ck8s.utils.VariablesUtils.*;

public final class VariablesCognitoTaskParams {

    private static final String PROFILE_KEY = "profile";
    private static final String REGION_KEY = "region";
    private static final String MAX_RESULTS = "maxResults";
    private static final String POOL_IDS_KEY = "poolIds";
    private static final String POOL_ID_KEY = "poolId";
    private static final String POOL_NAME_KEY = "poolName";
    private static final String POOL_CLIENT_NAME_KEY = "clientName";
    private static final String USERNAME_ATTRIBUTES_KEY = "usernameAttributes";
    private static final String USERNAME_CONFIGURATION_KEY = "usernameConfiguration";
    private static final String POLICY_KEY = "policy";
    private static final String POLICY_FILE_KEY = "policyFile";
    private static final String ADMIN_CREATE_USER_CONFIG_KEY = "adminCreateUserConfig";
    private static final String EMAIL_CONFIG_KEY = "emailConfiguration";
    private static final String SCHEMA_ATTRIBUTES_FILE_KEY = "schemaFile";
    private static final String TAGS_KEY = "tags";
    private static final String CLIENT_ID_KEY = "clientId";
    private static final String PROVIDER_NAME_KEY = "name";
    private static final String PROVIDER_TYPE_KEY = "type";
    private static final String PROVIDER_DETAILS_KEY = "details";
    private static final String PROVIDER_ATTRIBUTE_MAPPING_KEY = "attributeMapping";
    private static final String RESOURCE_SERVER_NAME_KEY = "name";
    private static final String RESOURCE_SERVER_IDENTIFIER_KEY = "identifier";
    private static final String RESOURCE_SERVER_SCOPES_KEY = "scopes";
    private static final String USER_POOL_CLIENT_NAME_KEY = "name";
    private static final String USER_POOL_CLIENT_GENERATE_SECRET_KEY = "generateSecret";
    private static final String USER_POOL_CLIENT_REFRESH_TOKEN_VALIDITY_KEY = "refreshTokenValidity";
    private static final String USER_POOL_CLIENT_ACCESS_TOKEN_VALIDITY_KEY = "accessTokenValidity";
    private static final String USER_POOL_CLIENT_ID_TOKEN_VALIDITY_KEY = "idTokenValidity";
    private static final String USER_POOL_CLIENT_TOKEN_VALIDITY_UNITS_KEY = "tokenValidityUnits";
    private static final String USER_POOL_CLIENT_SUPPORT_IDENTITY_PROVIDERS_KEY = "supportedIdentityProviders";
    private static final String USER_POOL_CLIENT_PREVENT_USER_EXISTENCE_ERRORS_KEY = "preventUserExistenceErrors";
    private static final String USER_POOL_CLIENT_ENABLE_TOKEN_REVOCATION_KEY = "enableTokenRevocation";
    private static final String USER_POOL_CLIENT_CALLBACK_URLS_KEY = "callbackUrls";
    private static final String USER_POOL_CLIENT_LOGOUT_URLS_KEY = "logoutUrls";
    private static final String USER_POOL_CLIENT_ALLOWED_OAUTH_FLOWS_USER_POOL_CLIENT_KEY = "allowedOAuthFlowsUserPoolClient";
    private static final String USER_POOL_CLIENT_ALLOWED_OAUTH_FLOWS_KEY = "allowedOAuthFlows";
    private static final String USER_POOL_CLIENT_ALLOWED_OAUTH_SCOPES_KEY = "allowedOAuthScopes";
    private static final String USER_POOL_CLIENT_EXPLICIT_AUTH_FLOWS_KEY = "explicitAuthFlows";
    private static final String USER_POOL_DOMAIN_KEY = "domain";
    private static final String CSS_FILE_KEY = "cssFile";
    private static final String IMAGE_FILE_KEY = "imageFile";
    private static final String USERNAME_KEY = "username";
    private static final String PASSWORD_KEY = "password";
    private static final String URLS_KEY = "urls";

    public static CreateUserPoolParams createUserPoolParams(Context context, Variables variables) {
        return new CreateUserPoolParams(
                baseParams(variables),
                assertPoolName(variables),
                assertUsernameAttributes(variables),
                assertUsernameConfiguration(variables),
                assertUserPoolPolicy(context.workingDirectory(), variables),
                assertAdminCreateUserConfig(variables),
                assertEmailConfiguration(variables),
                assertSchemaAttributes(context.workingDirectory(), variables),
                assertTags(variables)
        );
    }

    public static ListUserPoolsParams listUserPools(Variables variables) {
        return new CognitoTaskParams.ListUserPoolsParams(
                baseParams(variables),
                maxResults(variables, 60)
        );
    }

    public static CognitoTaskParams.FindUserPoolParams findUserPool(Variables variables) {
        return new CognitoTaskParams.FindUserPoolParams(
            baseParams(variables),
            assertPoolName(variables)
        );
    }

    public static CognitoTaskParams.GetUserPoolClientParams getUserPoolClient(Variables variables) {
        return new CognitoTaskParams.GetUserPoolClientParams(
            baseParams(variables),
            assertPoolId(variables),
            assertClientId(variables)
        );
    }

    public static CognitoTaskParams.FindUserPoolClientParams findUserPoolClient(Variables variables) {
        return new CognitoTaskParams.FindUserPoolClientParams(
            baseParams(variables),
            assertPoolId(variables),
            assertClientName(variables)
        );
    }

    public static DeleteUserPoolsParams deleteUserPools(Variables variables) {
        return new DeleteUserPoolsParams(
                baseParams(variables),
                assertPoolsIds(variables)
        );
    }

    public static DeleteUserPoolParams deleteUserPool(Variables variables) {
        return new DeleteUserPoolParams(
                baseParams(variables),
                assertPoolName(variables),
                maxResults(variables, 60)
        );
    }

    public static CognitoTaskParams.CreateIdentityProviderParams createIdentityProvider(Variables variables) {
        return new CreateIdentityProviderParams(
                baseParams(variables),
                assertPoolId(variables),
                assertProviderName(variables),
                assertProviderType(variables),
                assertProviderDetails(variables),
                assertProviderAttributesMapping(variables)
        );
    }

    public static CognitoTaskParams.CreateResourceServerParams createResourceServer(Variables variables) {
        return new CreateResourceServerParams(
                baseParams(variables),
                assertPoolId(variables),
                assertResourceServerName(variables),
                assertResourceServerIdentifier(variables),
                assertResourceServerScopes(variables)
        );
    }

    public static CognitoTaskParams.CreateUserPoolClientParams createUserPoolClient(Variables variables) {
        return new CreateUserPoolClientParams(
                baseParams(variables),
                assertPoolId(variables),
                assertUserPoolClientName(variables),
                variables.getBoolean(USER_POOL_CLIENT_GENERATE_SECRET_KEY, false),
                variables.assertInt(USER_POOL_CLIENT_REFRESH_TOKEN_VALIDITY_KEY),
                variables.assertInt(USER_POOL_CLIENT_ACCESS_TOKEN_VALIDITY_KEY),
                variables.assertInt(USER_POOL_CLIENT_ID_TOKEN_VALIDITY_KEY),
                assertTokenValidityUnits(variables),
                variables.getList(USER_POOL_CLIENT_SUPPORT_IDENTITY_PROVIDERS_KEY, List.of()),
                assertPreventUserExistenceErrorTypes(variables),
                variables.assertBoolean(USER_POOL_CLIENT_ENABLE_TOKEN_REVOCATION_KEY),
                variables.getList(USER_POOL_CLIENT_CALLBACK_URLS_KEY, List.of()),
                variables.getList(USER_POOL_CLIENT_LOGOUT_URLS_KEY, List.of()),
                variables.getBoolean(USER_POOL_CLIENT_ALLOWED_OAUTH_FLOWS_USER_POOL_CLIENT_KEY, false),
                getAllowedOAuthFlows(variables),
                variables.getList(USER_POOL_CLIENT_ALLOWED_OAUTH_SCOPES_KEY, List.of()),
                assertExplicitAuthFlows(variables)
        );
    }

    public static CognitoTaskParams.CreateUserPoolUICustomizationParams createUserPoolUICustomization(Path workDir, Variables variables) {
        return new CreateUserPoolUICustomizationParams(
                baseParams(variables),
                assertPoolId(variables),
                assertBinaryFile(workDir, variables, IMAGE_FILE_KEY),
                assertFile(workDir, variables, CSS_FILE_KEY)
        );
    }

    public static CognitoTaskParams.CreateUserPoolDomainParams createUserPoolDomain(Variables variables) {
        return new CreateUserPoolDomainParams(
                baseParams(variables),
                assertPoolId(variables),
                variables.assertString(USER_POOL_DOMAIN_KEY)
        );
    }

    public static CognitoTaskParams.CreateUserPoolUserParams addUserPoolUser(Variables variables) {
        return new CreateUserPoolUserParams(
                baseParams(variables),
                assertPoolId(variables),
                variables.assertString(USERNAME_KEY),
                variables.assertString(PASSWORD_KEY)
        );
    }

    public static CognitoTaskParams.UserPoolClientCallbacks createUserPoolClientCallbacks(Variables variables) {
        return new UserPoolClientCallbacks(
                baseParams(variables),
                assertPoolId(variables),
                assertClientId(variables),
                variables.assertList(URLS_KEY)
        );
    }

    private static List<ExplicitAuthFlowsType> assertExplicitAuthFlows(Variables variables) {
        return variables.assertList(USER_POOL_CLIENT_EXPLICIT_AUTH_FLOWS_KEY).stream()
                .map(s -> assertExplicitAuthFlowsType(String.valueOf(s)))
                .toList();
    }

    private static ExplicitAuthFlowsType assertExplicitAuthFlowsType(String value) {
        var result = ExplicitAuthFlowsType.fromValue(value);
        if (result == null || result == ExplicitAuthFlowsType.UNKNOWN_TO_SDK_VERSION) {
            throw new UserDefinedException("Unknown explicit auth flows type: " + value + ", allowed values: " + ExplicitAuthFlowsType.knownValues());
        }
        return result;
    }

    private static List<OAuthFlowType> getAllowedOAuthFlows(Variables variables) {
        return variables.getList(USER_POOL_CLIENT_ALLOWED_OAUTH_FLOWS_KEY, List.of()).stream()
                .map(s -> assertOAuthFlowType(String.valueOf(s)))
                .toList();
    }

    private static OAuthFlowType assertOAuthFlowType(String value) {
        var result = OAuthFlowType.fromValue(value);
        if (result == null || result == OAuthFlowType.UNKNOWN_TO_SDK_VERSION) {
            throw new UserDefinedException("Unknown OAuth flow type: " + value + ", allowed values: " + OAuthFlowType.knownValues());
        }
        return result;
    }

    private static PreventUserExistenceErrorTypes assertPreventUserExistenceErrorTypes(Variables variables) {
        var value = variables.assertString(USER_POOL_CLIENT_PREVENT_USER_EXISTENCE_ERRORS_KEY);
        var result = PreventUserExistenceErrorTypes.fromValue(value);
        if (result == null || result == PreventUserExistenceErrorTypes.UNKNOWN_TO_SDK_VERSION) {
            throw new UserDefinedException("Unknown change set type: " + value + ", allowed values: " + PreventUserExistenceErrorTypes.knownValues());
        }
        return result;
    }

    private static TokenValidityUnitsType assertTokenValidityUnits(Variables variables) {
        var attributes = variables.<String, Object>getMap(USER_POOL_CLIENT_TOKEN_VALIDITY_UNITS_KEY, Map.of());
        return AwsTaskUtils.deserialize(attributes, TokenValidityUnitsType.serializableBuilderClass()).build();
    }

    private static String assertUserPoolClientName(Variables variables) {
        return variables.getString(USER_POOL_CLIENT_NAME_KEY);
    }

    private static List<String> assertPoolsIds(Variables variables) {
        return variables.assertList(POOL_IDS_KEY);
    }

    private static BaseParams baseParams(Variables variables) {
        return new BaseParams(profile(variables), assertRegion(variables));
    }

    private static String profile(Variables variables) {
        return variables.getString(PROFILE_KEY);
    }

    private static Region assertRegion(Variables variables) {
        return Region.of(variables.assertString(REGION_KEY));
    }

    private static int maxResults(Variables variables, int defaultValue) {
        return variables.getInt(MAX_RESULTS, defaultValue);
    }

    private static String assertPoolId(Variables variables) {
        return variables.assertString(POOL_ID_KEY);
    }

    private static String assertProviderName(Variables variables) {
        return variables.assertString(PROVIDER_NAME_KEY);
    }

    private static String assertResourceServerName(Variables variables) {
        return variables.assertString(RESOURCE_SERVER_NAME_KEY);
    }

    private static String assertResourceServerIdentifier(Variables variables) {
        return variables.assertString(RESOURCE_SERVER_IDENTIFIER_KEY);
    }

    private static String assertProviderType(Variables variables) {
        return variables.assertString(PROVIDER_TYPE_KEY);
    }

    private static Map<String, String> assertProviderDetails(Variables variables) {
        return assertStringMap(variables, PROVIDER_DETAILS_KEY);
    }

    private static Map<String, String> assertProviderAttributesMapping(Variables variables) {
        return assertStringMap(variables, PROVIDER_ATTRIBUTE_MAPPING_KEY);
    }

    private static String assertPoolName(Variables variables) {
        return variables.assertString(POOL_NAME_KEY);
    }

    private static String assertClientId(Variables variables) {
        return variables.assertString(CLIENT_ID_KEY);
    }

    private static String assertClientName(Variables variables) {
        return variables.assertString(POOL_CLIENT_NAME_KEY);
    }

    private static List<UsernameAttributeType> assertUsernameAttributes(Variables variables) {
        var attributes = variables.assertVariable(USERNAME_ATTRIBUTES_KEY, Object.class);
        if (attributes instanceof List<?> l) {
            return l.stream()
                    .map(c -> assertUsernameAttributeType(c.toString()))
                    .toList();
        } else if (attributes instanceof String s) {
            return List.of(assertUsernameAttributeType(s));
        } else {
            throw new UserDefinedException("Unsupported username attribute value type: " + attributes.getClass() + ", expected string or list of strings");
        }
    }

    private static UsernameAttributeType assertUsernameAttributeType(String value) {
        var result = UsernameAttributeType.fromValue(value);
        if (result == null || result == UsernameAttributeType.UNKNOWN_TO_SDK_VERSION) {
            throw new UserDefinedException("Unknown username attribute: " + value + ", allowed values: " + UsernameAttributeType.knownValues());
        }
        return result;
    }

    private static UsernameConfigurationType assertUsernameConfiguration(Variables variables) {
        var attributes = variables.<String, Object>getMap(USERNAME_CONFIGURATION_KEY, Map.of());
        return UsernameConfigurationType.builder()
                .caseSensitive(MapUtils.getBoolean(attributes, "CaseSensitive", false))
                .build();
    }

    private static UserPoolPolicyType assertUserPoolPolicy(Path workDir, Variables variables) {
        if (variables.has(POLICY_KEY)) {
            return AwsTaskUtils.deserialize(variables.assertMap(POLICY_KEY), UserPoolPolicyType.serializableBuilderClass()).build();
        }

        var p = assertPath(workDir, variables, POLICY_FILE_KEY);
        return AwsTaskUtils.deserialize(p, UserPoolPolicyType.serializableBuilderClass()).build();
    }

    private static AdminCreateUserConfigType assertAdminCreateUserConfig(Variables variables) {
        var attributes = variables.<String, Object>getMap(ADMIN_CREATE_USER_CONFIG_KEY, Map.of());
        return AwsTaskUtils.deserialize(attributes, AdminCreateUserConfigType.serializableBuilderClass()).build();
    }

    private static EmailConfigurationType assertEmailConfiguration(Variables variables) {
        var attributes = variables.<String, Object>getMap(EMAIL_CONFIG_KEY, Map.of());
        return AwsTaskUtils.deserialize(attributes, EmailConfigurationType.serializableBuilderClass()).build();
    }

    private static List<SchemaAttributeType> assertSchemaAttributes(Path workDir, Variables variables) {
        var p = assertPath(workDir, variables, SCHEMA_ATTRIBUTES_FILE_KEY);
        return AwsTaskUtils.deserializeList(p, SchemaAttributeType.serializableBuilderClass()).stream()
                .map(SdkBuilder::build)
                .toList();
    }

    private static List<ResourceServerScopeType> assertResourceServerScopes(Variables variables) {
        var attributes = variables.<Map<String, Object>>getList(RESOURCE_SERVER_SCOPES_KEY, List.of());
        return AwsTaskUtils.deserializeList(attributes, ResourceServerScopeType.serializableBuilderClass()).stream()
                .map(SdkBuilder::build)
                .toList();
    }

    private static Map<String, String> assertTags(Variables variables) {
        return assertStringMap(variables, TAGS_KEY);
    }

    private static Map<String, String> assertStringMap(Variables variables, String variableName) {
        return variables.getMap(variableName, Map.of()).entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toUnmodifiableMap(
                        entry -> String.valueOf(entry.getKey()),
                        entry -> String.valueOf(entry.getValue())
                ));
    }
}
