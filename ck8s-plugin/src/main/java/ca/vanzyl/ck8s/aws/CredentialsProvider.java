package ca.vanzyl.ck8s.aws;

import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves which AWS role a call runs under.
 * <p/>
 * The provider holds no role of its own. It reads the role the flow established and caches
 * the sessions issued for it, nothing else.
 */
@Singleton
public class CredentialsProvider {

    private final static Logger log = LoggerFactory.getLogger(CredentialsProvider.class);

    /**
     * Flow variable holding the {@link StsAssumeRole} for the current frame.
     * <p/>
     * Double-underscore prefixed to keep it out of the way of user variables, following
     * the convention the runtime itself uses ({@code __retry_cfg}, {@code __frame_input_overrides}).
     */
    public static final String ASSUME_ROLE_VARIABLE = "__ck8s_aws_assumed_role_info";

    /**
     * Key under {@code clusterRequest.aws} holding the process-wide default role, the one
     * awsPrereqs assumes at the start. Not a frame variable: it has to outlive the flow
     * that picks it. No {@code __} prefix - this is config, not a flow variable.
     */
    public static final String DEFAULT_ROLE_KEY = "assumedRoleInfo";

    private final Object lock = new Object();

    /**
     * Sessions issued per role. Purely a cache: the role itself is scoped to the frame,
     * and {@link StsAssumeRole} is an immutable record, so it is safe as a key.
     */
    private final Map<StsAssumeRole, SessionCredentials> sessionCache = new HashMap<>();

    @Inject
    public CredentialsProvider() {
    }

    public AwsCredentialsProvider get(Context context) {
        return get(context, AwsTaskUtils.getProfile(context));
    }

    public AwsCredentialsProvider get(Context context, Variables input) {
        return get(context, AwsTaskUtils.getProfile(context, input));
    }

    /**
     * The role is looked up eagerly: the returned provider may be invoked by the AWS SDK
     * later (retries, async clients) on a thread that has no {@link Context}.
     */
    public AwsCredentialsProvider get(Context context, String profile) {
        StsAssumeRole role = scopedAssumeRole(context);
        if (role == null) {
            return getDefault(profile);
        }

        return () -> toAwsCredentials(sessionFor(role));
    }

    public AwsSessionCredentials getSessionCredentials(Context context) {
        StsAssumeRole role = scopedAssumeRole(context);
        if (role == null) {
            return null;
        }

        return toAwsCredentials(sessionFor(role));
    }

    public AwsCredentialsProvider getDefault(String profile) {
        return DefaultCredentialsProvider.builder()
                .profileName(profile)
                .build();
    }

    /**
     * The role to use for the calling frame.
     * <p/>
     * A frame variable wins: frame locals are visible to nested calls and are copied into
     * parallel branches, so a flow wrapped in a role - and every branch it forks - resolves
     * its own. Without one, the process-wide default picked in awsPrereqs applies; that one
     * is not a scope, so it lives in clusterRequest rather than in a frame.
     */
    // package-private for tests
    static StsAssumeRole scopedAssumeRole(Context context) {
        if (context == null) {
            return null;
        }

        StsAssumeRole scoped = asAssumeRole(context.variables().get(ASSUME_ROLE_VARIABLE), ASSUME_ROLE_VARIABLE);
        if (scoped != null) {
            return scoped;
        }

        return processDefaultRole(context);
    }

    private static StsAssumeRole processDefaultRole(Context context) {
        Object clusterRequest = context.variables().get("clusterRequest");
        if (!(clusterRequest instanceof Map<?, ?> cr)) {
            return null;
        }

        if (!(cr.get("aws") instanceof Map<?, ?> aws)) {
            return null;
        }

        return asAssumeRole(aws.get(DEFAULT_ROLE_KEY), "clusterRequest.aws." + DEFAULT_ROLE_KEY);
    }

    private static StsAssumeRole asAssumeRole(Object v, String where) {
        if (v == null) {
            return null;
        }

        if (v instanceof StsAssumeRole role) {
            return role;
        }

        throw new IllegalStateException("Invalid '" + where + "' value, expected: "
                + StsAssumeRole.class.getName() + ", got: " + v.getClass().getName());
    }

    private SessionCredentials sessionFor(StsAssumeRole role) {
        synchronized (lock) {
            SessionCredentials cached = sessionCache.get(role);
            if (cached == null || isExpiring(cached)) {
                cached = refreshCredentials(role);
                sessionCache.put(role, cached);
            }
            return cached;
        }
    }

    private static AwsSessionCredentials toAwsCredentials(SessionCredentials c) {
        return AwsSessionCredentials.create(c.accessKeyId(), c.secretAccessKey(), c.sessionToken());
    }

    private static boolean isExpiring(SessionCredentials c) {
        return Instant.now().isAfter(c.expiration().minusSeconds(60));
    }

    private SessionCredentials refreshCredentials(StsAssumeRole credentials) {
        log.info("Refreshing credentials for role '{}' with session '{}'", credentials.roleArn(), credentials.sessionName());

        try (var client = StsClient.builder()
                .region(Region.of(credentials.region()))
                .credentialsProvider(getDefault(credentials.profile()))
                .build()) {

            var response = client.assumeRole(AssumeRoleRequest.builder()
                    .roleArn(credentials.roleArn())
                    .roleSessionName(credentials.sessionName())
                    .build());

            log.info("Role assumed successfully, access key id '{}' expires at '{}'", response.credentials().accessKeyId(), response.credentials().expiration());

            return SessionCredentials.from(response.credentials());
        } catch (Exception e) {
            log.error("Failed to refresh credentials for role '{}'", credentials.roleArn(), e);
            throw new RuntimeException("Error assuming role: " + credentials.roleArn(), e);
        }
    }
}
