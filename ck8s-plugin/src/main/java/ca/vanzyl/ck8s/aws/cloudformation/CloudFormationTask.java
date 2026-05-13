package ca.vanzyl.ck8s.aws.cloudformation;

import ca.vanzyl.ck8s.actions.ActionUtils;
import ca.vanzyl.ck8s.actions.TaskActionExecutor;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Named("ck8sAwsCloudFormation")
@DryRunReady
public class CloudFormationTask implements Task {

    private final List<CloudFormationTaskAction<? extends CloudFormationTaskParams>> actions;
    private final Context context;

    @Inject
    public CloudFormationTask(List<CloudFormationTaskAction<?>> actions, Context context) {
        this.actions = ActionUtils.assertActions(actions);
        this.context = context;
    }

    @Override
    public TaskResult execute(Variables input) throws Exception {
        return TaskActionExecutor.execute(context, input, CloudFormationTaskAction.Action.class, actions, CloudFormationTask::toActionInput);
    }

    /**
     * Sanitizes a CloudFormation stack name to comply with AWS naming rules.
     */
    public static String sanitizeStackName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new UserDefinedException("Stack name cannot be null or empty");
        }

        // 1. Replace all characters that are not letters, numbers, or dashes with a dash
        String sanitized = rawName.replaceAll("[^a-zA-Z0-9-]", "-");

        // 2. If the first character is not a letter, prefix with 'A'
        if (!sanitized.substring(0, 1).matches("[a-zA-Z]")) {
            sanitized = "A" + sanitized;
        }

        // 3. Truncate to a maximum length of 128 characters
        if (sanitized.length() > 128) {
            sanitized = sanitized.substring(0, 128);
        }

        return sanitized;
    }

    public static String resourcePart(String rawString) {
        if (rawString == null || rawString.isBlank()) {
            throw new UserDefinedException("Resource part cannot be null or empty");
        }

        String result = Arrays.stream(rawString.split("[^A-Za-z0-9]+"))
                .filter(s -> !s.isEmpty())
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                .collect(Collectors.joining());

        if (result.isBlank()) {
            throw new UserDefinedException("Can't create resource part from string: '" + rawString + "'");
        }
        return result;
    }

    private static CloudFormationTaskParams toActionInput(CloudFormationTaskAction.Action action, Variables variables) {
        return switch (action) {
            case CREATE -> VariablesCloudFormationTaskParams.create(variables);
            case GET_TEMPLATE -> VariablesCloudFormationTaskParams.getTemplate(variables);
            case CREATE_CHANGE_SET -> VariablesCloudFormationTaskParams.createChangeSet(variables);
            case DELETE -> VariablesCloudFormationTaskParams.delete(variables);
            case DEPLOY -> VariablesCloudFormationTaskParams.deploy(variables);
            case EXECUTE_CHANGE_SET -> VariablesCloudFormationTaskParams.executeChangeSet(variables);
            case EXISTS -> VariablesCloudFormationTaskParams.exists(variables);
        };
    }
}
