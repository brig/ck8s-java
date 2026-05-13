package ca.vanzyl.ck8s.aws.cloudformation.actions;

import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationClientFactory;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTaskAction;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudformation.model.ChangeSetType;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.CreateChangeSetRequest;

import javax.inject.Inject;

import static ca.vanzyl.ck8s.aws.cloudformation.CloudFormationUtils.handleNoUpdates;
import static ca.vanzyl.ck8s.aws.cloudformation.CloudFormationUtils.waitForChangeSetCreated;

public class CreateChangeSetAction extends CloudFormationTaskAction<CloudFormationTaskParams.CreateChangeSetParams> {

    private static final Logger log = LoggerFactory.getLogger(CreateChangeSetAction.class);

    @Inject
    public CreateChangeSetAction(CloudFormationClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.CREATE_CHANGE_SET;
    }

    @Override
    public TaskResult execute(Context context, CloudFormationTaskParams.CreateChangeSetParams input) {
        var stackName = input.stackName();
        var changeSetName = input.changeSetName();
        var changeSetType = input.changeSetType();
        var templateBody = input.templateBody();
        var parameters = input.parameterOverrides();
        var capabilities = input.capabilities();

        try (var client = createClient(input)) {
            CreateChangeSetRequest.Builder request = CreateChangeSetRequest.builder()
                    .stackName(stackName)
                    .changeSetName(changeSetName)
                    .changeSetType(changeSetType)
                    .capabilities(capabilities)
                    .templateBody(templateBody)
                    .parameters(parameters);

            if (changeSetType == ChangeSetType.IMPORT) {
                request = request.resourcesToImport(input.resourcesToImport());
            } else {
                throw new UserDefinedException("Unsupported change set type '" + changeSetType);
            }

            client.createChangeSet(request.build());

            log.info("CloudFormation stack '{}' change set '{}' create initiated...", stackName, changeSetName);

            waitForChangeSetCreated(client, stackName, changeSetName);

            return TaskResult.success();
        } catch (CloudFormationException e) {
            return handleNoUpdates("Failed to create Cloud Formation change set: ", e);
        }
    }
}
