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

public class GetTemplateAction extends CloudFormationTaskAction<CloudFormationTaskParams.GetTemplateParams> {

    private static final Logger log = LoggerFactory.getLogger(GetTemplateAction.class);

    @Inject
    public GetTemplateAction(CloudFormationClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.GET_TEMPLATE;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, CloudFormationTaskParams.GetTemplateParams input) {
        var stackName = input.stackName();
        try (var client = createClient(input)) {
            var body = client.getTemplate(r -> r.stackName(stackName)).templateBody();

            return TaskResult.success()
                    .value("template", body);
        } catch (AwsServiceException e) {
            if ("ValidationError".equals(e.awsErrorDetails().errorCode())) {
                log.info("Cloud Formation '{}' does not exists",stackName);
                return TaskResult.success();
            }
            log.error("❌ Failed to check Cloud Formation: {}", e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }
}
