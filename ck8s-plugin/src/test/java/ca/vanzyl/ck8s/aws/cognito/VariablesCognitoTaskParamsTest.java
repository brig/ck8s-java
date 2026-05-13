package ca.vanzyl.ck8s.aws.cognito;

import ca.vanzyl.ck8s.MockTestContext;
import ca.vanzyl.ck8s.common.Mapper;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ExplicitAuthFlowsType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.OAuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.PreventUserExistenceErrorTypes;
import software.amazon.awssdk.services.cognitoidentityprovider.model.TimeUnitsType;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class VariablesCognitoTaskParamsTest {

    @Test
    public void testCreateUserPoolParams() {
        var inviteMessageTemplate = Map.of("SmsMessage", "Your Aetion username is {username} and temporary password is {####}.",
        "EmailMessage", "Your Aetion username is {username} and temporary password is {####}.",
        "EmailSubject", "Your Aetion temporary password");

        var usernameAttributes = "email";
        var m = Map.of(
                "region", "us-east-1",
                "poolName", "test-pool",
                "usernameAttributes", usernameAttributes,
                "usernameConfiguration", Map.of("CaseSensitive", true),
                "policy", jsonResource("user-pool-policy.json"),
                "adminCreateUserConfig", Map.of("AllowAdminCreateUserOnly", true, "UnusedAccountValidityDays", 7, "InviteMessageTemplate", inviteMessageTemplate),
                "emailConfiguration", Map.of("SourceArn", "arn:aws:iam::123456789012:user-pool/123456789012", "EmailSendingAccount", "DEVELOPER"),
                "schemaFile", assertResource("schema-custom-attributes.json").toString(),
                "tags", Map.of("Name", "TEST")
        );

        var parsed = VariablesCognitoTaskParams.createUserPoolParams(new MockTestContext(Path.of("/")), new MapBackedVariables(m));

        assertEquals(1, parsed.usernameAttributes().size());
        assertEquals(usernameAttributes, parsed.usernameAttributes().get(0).toString());

        assertTrue(parsed.usernameConfiguration().caseSensitive());

        var parsedPolicy = parsed.policy().passwordPolicy();
        assertEquals((Integer)8, parsedPolicy.minimumLength());
        assertTrue(parsedPolicy.requireUppercase());
        assertTrue(parsedPolicy.requireLowercase());
        assertTrue(parsedPolicy.requireNumbers());
        assertFalse(parsedPolicy.requireSymbols());
        assertEquals((Integer)90, parsedPolicy.temporaryPasswordValidityDays());

        assertTrue(parsed.adminCreateUserConfig().allowAdminCreateUserOnly());

        assertEquals("arn:aws:iam::123456789012:user-pool/123456789012", parsed.emailConfiguration().sourceArn());
        assertEquals("DEVELOPER", parsed.emailConfiguration().emailSendingAccountAsString());

        assertEquals(6, parsed.schema().size());

        var g1 = parsed.schema().get(0);
        assertEquals("ad-groups", g1.name());
        assertEquals("String", g1.attributeDataTypeAsString());
        assertEquals(false, g1.developerOnlyAttribute());
        assertTrue(g1.mutable());
        assertFalse(g1.required());
        assertEquals("1", g1.stringAttributeConstraints().minLength());
        assertEquals("256", g1.stringAttributeConstraints().maxLength());

        assertEquals(1, parsed.tags().size());
        assertEquals("TEST", parsed.tags().get("Name"));
    }

    @Test
    public void testCreateResourceServerParams() {
        var m = Map.<String, Object>of(
                "region", "us-east-1",
                "poolId", "test-pool-id",
                "name", "server-name",
                "identifier", "server-id",
                "scopes", List.of(Map.of("ScopeName", "instance", "ScopeDescription", "Id"))
        );

        var parsed = VariablesCognitoTaskParams.createResourceServer(new MapBackedVariables(m));
        System.out.println(parsed);

        assertEquals(Region.US_EAST_1, parsed.baseParams().region());
        assertEquals("test-pool-id", parsed.poolId());
        assertEquals("server-name", parsed.name());
        assertEquals("server-id", parsed.identifier());
        assertEquals(1, parsed.scopes().size());
        assertEquals("instance", parsed.scopes().get(0).scopeName());
        assertEquals("Id", parsed.scopes().get(0).scopeDescription());
    }

    @Test
    public void testCreateClientParams() {
        var m = new HashMap<String, Object>();
        m.put("region", "us-east-1");
        m.put("poolId", "test-pool-id");
        m.put("name", "client-name");
        m.put("generateSecret", true);
        m.put("supportedIdentityProviders", List.of("TEST1", "TEST2"));
        m.put("preventUserExistenceErrors", "ENABLED");
        m.put("enableTokenRevocation", true);
        m.put("refreshTokenValidity", 1);
        m.put("accessTokenValidity", 2);
        m.put("idTokenValidity", 3);
        m.put("callbackUrls", List.of("c1", "c2"));
        m.put("logoutUrls", List.of("l1", "l2"));
        m.put("allowedOAuthFlowsUserPoolClient", true);
        m.put("allowedOAuthFlows", List.of("code", "implicit"));
        m.put("allowedOAuthScopes", List.of("email", "openid", "profile"));
        m.put("tokenValidityUnits", Map.of("AccessToken", "minutes", "IdToken", "minutes", "RefreshToken", "days"));
        m.put("explicitAuthFlows", List.of("ALLOW_CUSTOM_AUTH", "ALLOW_USER_PASSWORD_AUTH", "ALLOW_USER_SRP_AUTH", "ALLOW_REFRESH_TOKEN_AUTH"));

        var parsed = VariablesCognitoTaskParams.createUserPoolClient(new MapBackedVariables(m));
        System.out.println(parsed);

        assertEquals(Region.US_EAST_1, parsed.baseParams().region());
        assertEquals("test-pool-id", parsed.poolId());
        assertEquals("client-name", parsed.name());
        assertTrue(parsed.generateSecret());
        assertEquals(List.of("TEST1", "TEST2"), parsed.supportedIdentityProviders());
        assertEquals(PreventUserExistenceErrorTypes.ENABLED, parsed.preventUserExistenceErrors());
        assertTrue(parsed.enableTokenRevocation());

        assertEquals((Integer)1, parsed.refreshTokenValidity());
        assertEquals((Integer)2, parsed.accessTokenValidity());
        assertEquals((Integer)3, parsed.idTokenValidity());
        assertEquals(List.of("c1", "c2"), parsed.callbackURLs());
        assertEquals(List.of("l1", "l2"), parsed.logoutURLs());

        assertTrue(parsed.allowedOAuthFlowsUserPoolClient());
        assertEquals(List.of(OAuthFlowType.CODE, OAuthFlowType.IMPLICIT), parsed.allowedOAuthFlows());
        assertEquals(List.of("email", "openid", "profile"), parsed.allowedOAuthScopes());

        assertEquals(TimeUnitsType.MINUTES, parsed.tokenValidityUnits().accessToken());
        assertEquals(TimeUnitsType.MINUTES, parsed.tokenValidityUnits().idToken());
        assertEquals(TimeUnitsType.DAYS, parsed.tokenValidityUnits().refreshToken());

        assertEquals(List.of(
                ExplicitAuthFlowsType.ALLOW_CUSTOM_AUTH,
                ExplicitAuthFlowsType.ALLOW_USER_PASSWORD_AUTH,
                ExplicitAuthFlowsType.ALLOW_USER_SRP_AUTH,
                ExplicitAuthFlowsType.ALLOW_REFRESH_TOKEN_AUTH
        ), parsed.explicitAuthFlows());
    }

    private static Object jsonResource(String resource) {
        var p = assertResource(resource);
        return Mapper.json().readMap(p);
    }

    private static Path assertResource(String resource) {
        var r = VariablesCognitoTaskParams.class.getResource(resource);
        if (r == null) {
            throw new RuntimeException("Resource not found: " + resource);
        }

        try {
            return Path.of(r.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
