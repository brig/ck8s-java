package ca.vanzyl.ck8s.aws.cloudformation.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationClientFactory;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTaskAction;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

import javax.inject.Inject;
import java.util.Set;

import static ca.vanzyl.ck8s.aws.cloudformation.CloudFormationUtils.stackExists;

public class ExistsAction extends CloudFormationTaskAction<CloudFormationTaskParams.ExistsParams> {

    private static final Logger log = LoggerFactory.getLogger(ExistsAction.class);

    @Inject
    public ExistsAction(CloudFormationClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.EXISTS;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, CloudFormationTaskParams.ExistsParams input) {
        var stackName = input.stackName();
        try (var client = createClient(input)) {
            var exists = stackExists(client, stackName);

            log.info("CloudFormation stack '{}' {}", stackName, exists ? "exists" : "does not exist");

            return TaskResult.success()
                    .value("exists", exists);
        } catch (AwsServiceException e) {
            log.error("❌ Failed to check Cloud Formation: {}", e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }
}
