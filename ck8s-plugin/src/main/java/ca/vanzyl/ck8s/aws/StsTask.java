package ca.vanzyl.ck8s.aws;

import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import javax.inject.Inject;
import javax.inject.Named;

import static ca.vanzyl.ck8s.aws.AwsTaskUtils.assertRegion;
import static ca.vanzyl.ck8s.aws.AwsTaskUtils.getProfile;

@DryRunReady
@Named("ck8sAwsSts")
public class StsTask implements Task {

    private final static Logger log = LoggerFactory.getLogger(StsTask.class);

    private final CredentialsProvider credentialsProvider;
    private final Context context;

    @Inject
    public StsTask(CredentialsProvider credentialsProvider, Context context) {
        this.credentialsProvider = credentialsProvider;
        this.context = context;
    }

    /**
     * The role the caller is running under: the one a wrapper flow established for this
     * frame, or the process-wide default picked in awsPrereqs. {@code null} if neither.
     * <p/>
     * For use from an expression, so that flows do not have to know where the role is kept
     * or in which order the two places are consulted:
     * <pre>${ck8sAwsSts.currentRole()}</pre>
     */
    public StsAssumeRole currentRole() {
        return CredentialsProvider.currentRole(context);
    }

    /**
     * ARN of {@link #currentRole()}, or {@code null}. Usually the only part a flow needs:
     * <pre>${ck8sAwsSts.currentRoleArn()}</pre>
     */
    public String currentRoleArn() {
        var role = currentRole();
        return role != null ? role.roleArn() : null;
    }

    @Override
    @SensitiveData(keys = "sessionToken")
    public TaskResult execute(Variables input) throws Exception {
        var action = input.assertString("action");
        if ("assume-role".equals(action)) {
            return assumeRole(input);
        } else if ("cleanup".equals(action)) {
            return cleanup(input);
        }
        throw new IllegalArgumentException("Unsupported action: " + action);
    }

    private TaskResult cleanup(Variables input) {
        log.info("Cleanup assume role info");
        credentialsProvider.setCredentials(null, null);
        return TaskResult.success();
    }

    private TaskResult assumeRole(Variables input) {
        var roleArn = input.assertString("roleArn");
        var roleSessionName = input.assertString("roleSessionName");

        log.info("Assuming role '{}' with session '{}'", roleArn, roleSessionName);

        try (var client = StsClient.builder()
                .region(assertRegion(context, input))
                .credentialsProvider(credentialsProvider.getDefault(getProfile(context, input)))
                .build()) {

            var response = client.assumeRole(AssumeRoleRequest.builder()
                    .roleArn(roleArn)
                    .roleSessionName(roleSessionName)
                    .build());

            log.info("Role assumed:\n" +
                     "\troleId: {}\n" +
                     "\tarn: {}\n" +
                     "\texpire: {}",
                    response.assumedRoleUser().assumedRoleId(), response.assumedRoleUser().arn(),
                    response.credentials().expiration());

            var assumeRole = StsAssumeRole.from(getProfile(input), assertRegion(input), roleArn, roleSessionName);

            // still published process-wide so that flows which have not been migrated keep working
            credentialsProvider.setCredentials(SessionCredentials.from(response.credentials()), assumeRole);

            // the caller is expected to bind this to CredentialsProvider.ASSUME_ROLE_VARIABLE in the
            // frame that should run under the role. The task cannot do it itself: a flow call creates
            // its own root frame, so a variable set here would die with this task's calling flow.
            return TaskResult.success()
                    .value("sessionToken", response.credentials().sessionToken())
                    .value("assumeRole", assumeRole)
                    .value("credentials", AwsTaskUtils.serialize(response.credentials()));
        } catch (Exception e) {
            log.error("Error assuming role '{}'", roleArn, e);
            return TaskResult.fail(e.getMessage());
        }
    }
}
