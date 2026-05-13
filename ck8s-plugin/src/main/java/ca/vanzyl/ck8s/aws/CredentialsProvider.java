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
import java.util.UUID;

import static ca.vanzyl.ck8s.aws.AwsTaskUtils.getProfile;

@Singleton
public class CredentialsProvider implements ExecutionListener {

    private final static Logger log = LoggerFactory.getLogger(CredentialsProvider.class);

    private static final String ASSUME_ROLE_FILENAME = "assume-role-%s.json";

    private final Object lock = new Object();

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
        return get(AwsTaskUtils.getProfile(context));
    }

    public AwsCredentialsProvider get(Context context, Variables input) {
        return get(AwsTaskUtils.getProfile(context, input));
    }

    public AwsCredentialsProvider get(Variables input) {
        return get(getProfile(input));
    }

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

    public AwsSessionCredentials getSessionCredentials() {
        synchronized (lock) {
            if (assumeRole == null) {
                return null;
            }

            refreshSessionIfNeeded();

            return AwsSessionCredentials.create(
                    sessionCredentials.accessKeyId(),
                    sessionCredentials.secretAccessKey(),
                    sessionCredentials.sessionToken()
            );
        }
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
