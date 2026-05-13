package ca.vanzyl.ck8s.aws.cloudformation.actions;

import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationClientFactory;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTaskAction;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.ExecuteChangeSetRequest;

import javax.inject.Inject;
import java.util.stream.Stream;

import static ca.vanzyl.ck8s.aws.cloudformation.CloudFormationUtils.*;

public class ExecuteChangeSetAction extends CloudFormationTaskAction<CloudFormationTaskParams.ExecuteChangeSetParams> {

    private static final Logger log = LoggerFactory.getLogger(ExecuteChangeSetAction.class);

    @Inject
    public ExecuteChangeSetAction(CloudFormationClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.EXECUTE_CHANGE_SET;
    }

    @Override
    public TaskResult execute(Context context, CloudFormationTaskParams.ExecuteChangeSetParams input) {
        var stackName = input.stackName();
        var changeSetName = input.changeSetName();

        try (var client = createClient(input)) {
            log.info("CloudFormation stack '{}' change set '{}' execute initiated...", stackName, changeSetName);

            client.executeChangeSet(ExecuteChangeSetRequest.builder()
                    .stackName(stackName)
                    .changeSetName(changeSetName)
                    .build());

            var okStatuses = Stream.concat(STACK_IMPORT_OK_STATUSES.stream(), STACK_UPDATE_OK_STATUSES.stream()).toList();
            var errorStatuses = Stream.concat(STACK_IMPORT_ERROR_STATUSES.stream(), STACK_UPDATE_ERROR_STATUSES.stream()).toList();

            waitForStackCompletion(client, stackName, okStatuses, errorStatuses, "update/import");

            return TaskResult.success();
        } catch (CloudFormationException e) {
            return handleNoUpdates("Failed to execute Cloud Formation change set: ", e);
        }
    }
}
