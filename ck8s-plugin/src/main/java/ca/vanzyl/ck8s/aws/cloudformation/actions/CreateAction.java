package ca.vanzyl.ck8s.aws.cloudformation.actions;

import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationClientFactory;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTaskAction;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;

import javax.inject.Inject;

import static ca.vanzyl.ck8s.aws.cloudformation.CloudFormationUtils.*;

public class CreateAction extends CloudFormationTaskAction<CloudFormationTaskParams.CreateParams> {

    private static final Logger log = LoggerFactory.getLogger(CreateAction.class);

    @Inject
    public CreateAction(CloudFormationClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.CREATE;
    }

    @Override
    public TaskResult execute(Context context, CloudFormationTaskParams.CreateParams input) {
        var stackName = input.stackName();
        var capabilities = input.capabilities();
        var parameters = input.parameterOverrides();
        var templateBody = input.templateBody();

        try (var client = createClient(input)) {

            client.createStack(CreateStackRequest.builder()
                    .stackName(stackName)
                    .templateBody(templateBody)
                    .capabilities(capabilities)
                    .parameters(parameters)
                    .build());

            log.info("CloudFormation stack '{}' create initiated...", stackName);

            waitForStackCompletion(client, stackName, STACK_CREATE_OK_STATUSES, STACK_CREATE_ERROR_STATUSES, "update");

            return TaskResult.success();
        } catch (CloudFormationException e) {
            return handleNoUpdates("Failed to create Cloud Formation: ", e);
        }
    }
}
