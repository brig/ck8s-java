package ca.vanzyl.ck8s.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.runtime.common.injector.InstanceId;
import com.walmartlabs.concord.runtime.v2.runner.PersistenceService;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import com.walmartlabs.concord.svm.*;
import com.walmartlabs.concord.svm.Runtime;
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
import java.util.UUID;

import static ca.vanzyl.ck8s.aws.AwsTaskUtils.getProfile;

@Singleton
public class CredentialsProvider implements ExecutionListener {

    private final static Logger log = LoggerFactory.getLogger(CredentialsProvider.class);

    private static final String ASSUME_ROLE_FILENAME = "assume-role-%s.json";

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

    private final ObjectMapper objectMapper;
    private final PersistenceService persistenceService;
    private final UUID instanceId;

    private SessionCredentials sessionCredentials;
    private StsAssumeRole assumeRole;

    @Inject
    public CredentialsProvider(ObjectMapper objectMapper, PersistenceService persistenceService,  InstanceId instanceId) {
        this.objectMapper = objectMapper;
        this.persistenceService = persistenceService;
        this.instanceId = instanceId.getValue();
    }

    @Override
    public void beforeProcessResume(Runtime runtime, State state) {
        this.assumeRole = persistenceService.loadPersistedFile(filename(),
                is -> objectMapper.readValue(is, StsAssumeRole.class));
    }

    @Override
    public void onProcessError(Runtime runtime, State state, Exception e) {
        cleanupState();
    }

    @Override
    public void afterProcessEnds(Runtime runtime, State state, Frame lastFrame) {
        cleanupState();

        if (!isSuspended(state) || assumeRole == null) {
            return;
        }

        persistenceService.persistFile(filename(),
                out -> objectMapper.writeValue(out, this.assumeRole));
    }

    public void setCredentials(SessionCredentials sessionCredentials, StsAssumeRole assumeRole) {
        synchronized (lock) {
            this.sessionCredentials = sessionCredentials;
            this.assumeRole = assumeRole;
        }
    }

    public AwsCredentialsProvider get(Context context) {
        return get(context, AwsTaskUtils.getProfile(context));
    }

    public AwsCredentialsProvider get(Context context, Variables input) {
        return get(context, AwsTaskUtils.getProfile(context, input));
    }

    /**
     * Resolves the role from the calling frame, falling back to the process-wide
     * state for flows that have not been migrated yet.
     * <p/>
     * The role is looked up eagerly: the returned provider may be invoked by the AWS
     * SDK later (retries, async clients) on a thread that has no {@link Context}.
     */
    public AwsCredentialsProvider get(Context context, String profile) {
        StsAssumeRole scoped = scopedAssumeRole(context);
        if (scoped == null) {
            return get(profile);
        }

        return () -> toAwsCredentials(sessionFor(scoped));
    }

    /**
     * @deprecated relies on process-wide state, which is shared by parallel branches.
     * Use {@link #get(Context, String)}.
     */
    @Deprecated
    public AwsCredentialsProvider get(Variables input) {
        return get(getProfile(input));
    }

    /**
     * @deprecated see {@link #get(Variables)}.
     */
    @Deprecated
    public AwsCredentialsProvider get(String profile) {
        synchronized (lock) {
            if (assumeRole == null) {
                return getDefault(profile);
            }

            refreshSessionIfNeeded();

            return () -> AwsSessionCredentials.create(
                    sessionCredentials.accessKeyId(),
                    sessionCredentials.secretAccessKey(),
                    sessionCredentials.sessionToken()
            );
        }
    }

    public AwsSessionCredentials getSessionCredentials(Context context) {
        StsAssumeRole scoped = scopedAssumeRole(context);
        if (scoped == null) {
            return getSessionCredentials();
        }

        return toAwsCredentials(sessionFor(scoped));
    }

    /**
     * @deprecated see {@link #get(Variables)}.
     */
    @Deprecated
    public AwsSessionCredentials getSessionCredentials() {
        synchronized (lock) {
            if (assumeRole == null) {
                return null;
            }

            refreshSessionIfNeeded();

            return toAwsCredentials(sessionCredentials);
        }
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

    public AwsCredentialsProvider getDefault(String profile) {
        return DefaultCredentialsProvider.builder()
                .profileName(profile)
                .build();
    }

    private void refreshSessionIfNeeded() {
        if (sessionCredentials == null || Instant.now().isAfter(sessionCredentials.expiration().minusSeconds(60))) {
            sessionCredentials = refreshCredentials(assumeRole);
        }
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

    private void cleanupState() {
        try {
            persistenceService.deletePersistedFile(filename());
        } catch (Exception e) {
            // ignore
            log.warn("Can't delete assume role file", e);
        }
    }

    private String filename() {
        return String.format(ASSUME_ROLE_FILENAME, instanceId);
    }

    private static boolean isSuspended(State state) {
        return state.threadStatus().entrySet().stream()
                .anyMatch(e -> e.getValue() == ThreadStatus.SUSPENDED);
    }
}
