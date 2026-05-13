package ca.vanzyl.ck8s.aws.cloudformation.actions;

import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationClientFactory;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTaskAction;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;

import javax.inject.Inject;

import static ca.vanzyl.ck8s.aws.cloudformation.CloudFormationUtils.waitForStackDelete;

public class DeleteAction extends CloudFormationTaskAction<CloudFormationTaskParams.DeleteParams> {

    private static final Logger log = LoggerFactory.getLogger(DeleteAction.class);

    @Inject
    public DeleteAction(CloudFormationClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.DELETE;
    }

    @Override
    public TaskResult execute(Context context, CloudFormationTaskParams.DeleteParams input) {
        var stackName = input.stackName();
        var retainResources = input.retainResources();

        try (var client = createClient(input)) {

            log.info("Initiating deletion of stack: {}", stackName);

            client.deleteStack(DeleteStackRequest.builder()
                    .stackName(stackName)
                    .retainResources(retainResources)
                    .build());

            waitForStackDelete(client, stackName);

            return TaskResult.success();
        } catch (CloudFormationException e) {
            log.error("❌ Failed to delete stack: {}", e.awsErrorDetails().errorMessage(), e);
            return TaskResult.fail(e);
        }
    }
}
