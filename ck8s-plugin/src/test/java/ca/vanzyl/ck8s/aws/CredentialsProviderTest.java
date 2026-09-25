package ca.vanzyl.ck8s.aws;

import ca.vanzyl.ck8s.MockTestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.runtime.common.injector.InstanceId;
import com.walmartlabs.concord.runtime.v2.runner.PersistenceService;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.util.Map;
import java.util.UUID;

import static ca.vanzyl.ck8s.aws.CredentialsProvider.ASSUME_ROLE_VARIABLE;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * The role has to come from the calling frame rather than from process-wide state:
 * parallel branches share the process, so a single slot means one branch runs under
 * another branch's role.
 */
public class CredentialsProviderTest {

    private static final StsAssumeRole ADMIN =
            StsAssumeRole.from("default", Region.US_EAST_1, "arn:aws:iam::1:role/admin", "concord-agent-admin");
    private static final StsAssumeRole APP =
            StsAssumeRole.from("default", Region.US_EAST_1, "arn:aws:iam::1:role/app", "concord-agent-app");

    private static CredentialsProvider provider() {
        return new CredentialsProvider(new ObjectMapper(), mock(PersistenceService.class), new InstanceId(UUID.randomUUID()));
    }

    @Test
    public void noRoleInFrameMeansNoScopedRole() {
        assertNull(CredentialsProvider.currentRole(new MockTestContext(Map.of())));
    }

    @Test
    public void roleIsReadFromTheFrame() {
        var ctx = new MockTestContext(Map.of(ASSUME_ROLE_VARIABLE, ADMIN));

        assertEquals(ADMIN, CredentialsProvider.currentRole(ctx));
    }

    /**
     * The case that used to break: two parallel branches under different roles.
     */
    @Test
    public void branchesDoNotSeeEachOthersRole() {
        var branchA = new MockTestContext(Map.of(ASSUME_ROLE_VARIABLE, ADMIN));
        var branchB = new MockTestContext(Map.of(ASSUME_ROLE_VARIABLE, APP));

        assertEquals(ADMIN, CredentialsProvider.currentRole(branchA));
        assertEquals(APP, CredentialsProvider.currentRole(branchB));
    }

    /**
     * awsPrereqs assumes a role for the whole process. It is not a scope, so it lives in
     * clusterRequest instead of a frame.
     */
    @Test
    public void theProcessDefaultAppliesWhenTheFrameNamesNoRole() {
        var ctx = new MockTestContext(Map.of("clusterRequest", Map.of("aws", Map.of("initialAssumedRoleInfo", APP))));

        assertEquals(APP, CredentialsProvider.currentRole(ctx));
    }

    @Test
    public void aScopedRoleWinsOverTheProcessDefault() {
        var ctx = new MockTestContext(Map.of(
                ASSUME_ROLE_VARIABLE, ADMIN,
                "clusterRequest", Map.of("aws", Map.of("initialAssumedRoleInfo", APP))));

        assertEquals(ADMIN, CredentialsProvider.currentRole(ctx));
    }

    @Test
    public void aClusterRequestWithoutARoleIsNotAnError() {
        var ctx = new MockTestContext(Map.of("clusterRequest", Map.of("aws", Map.of("homeRegion", "us-east-1"))));

        assertNull(CredentialsProvider.currentRole(ctx));
    }

    @Test
    public void aWrongTypeIsReportedWithTheVariableName() {
        var ctx = new MockTestContext(Map.of(ASSUME_ROLE_VARIABLE, "arn:aws:iam::1:role/admin"));

        var e = assertThrows(IllegalStateException.class, () -> CredentialsProvider.currentRole(ctx));
        assertTrue(e.getMessage(), e.getMessage().contains(ASSUME_ROLE_VARIABLE));
    }

    /**
     * Without a role in the frame nothing is assumed and no STS call is made: flows that
     * have not been migrated keep using the default chain.
     */
    @Test
    public void fallsBackToTheDefaultChainWhenNothingIsAssumed() {
        var credentials = provider().get(new MockTestContext(Map.of()), "default");

        assertTrue(credentials.getClass().getName(), credentials instanceof DefaultCredentialsProvider);
    }
}
