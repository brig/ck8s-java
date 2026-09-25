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

    @Override
    @SensitiveData(keys = "sessionToken")
    public TaskResult execute(Variables input) throws Exception {
        var action = input.assertString("action");
        if ("assume-role".equals(action)) {
            return assumeRole(input);
        }
        throw new IllegalArgumentException("Unsupported action: " + action);
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
