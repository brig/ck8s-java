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
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;

import javax.inject.Inject;

import static ca.vanzyl.ck8s.aws.cloudformation.CloudFormationUtils.*;

public class DeployAction extends CloudFormationTaskAction<CloudFormationTaskParams.DeployParams> {

    private static final Logger log = LoggerFactory.getLogger(DeployAction.class);

    @Inject
    public DeployAction(CloudFormationClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.DEPLOY;
    }

    @Override
    public TaskResult execute(Context context, CloudFormationTaskParams.DeployParams input) {
        var stackName = input.stackName();
        var capabilities = input.capabilities();
        var parameters = input.parameterOverrides();
        var templateBody = input.templateBody();

        try (var client = createClient(input)) {
            var stackExists = stackExists(client, stackName);

            if (stackExists) {
                log.info("CloudFormation stack '{}' exists. Updating it...", stackName);

                client.updateStack(UpdateStackRequest.builder()
                        .stackName(stackName)
                        .templateBody(templateBody)
                        .capabilities(capabilities)
                        .parameters(parameters)
                        .build());

                log.info("CloudFormation stack '{}' update initiated...", stackName);

                waitStackUpdated(client, stackName);

            } else {
                log.info("CloudFormation stack '{}' does not exists. Creating it...", stackName);

                client.createStack(CreateStackRequest.builder()
                        .stackName(stackName)
                        .templateBody(templateBody)
                        .capabilities(capabilities)
                        .parameters(parameters)
                        .build());

                log.info("CloudFormation stack '{}' creation initiated...", stackName);

                waitForStackCreated(client, stackName);
            }

            return TaskResult.success();
        } catch (CloudFormationException e) {
            return handleNoUpdates("Failed to deploy Cloud Formation: ", e);
        }
    }
}
