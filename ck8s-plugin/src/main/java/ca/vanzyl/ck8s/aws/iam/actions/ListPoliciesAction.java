package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.aws.AwsTaskUtils;
import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.model.IamException;
import software.amazon.awssdk.services.iam.model.ListPoliciesRequest;

import javax.inject.Inject;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.ListPoliciesParams;

public class ListPoliciesAction extends IamTaskAction<ListPoliciesParams> {

    private final static Logger log = LoggerFactory.getLogger(ListPoliciesAction.class);

    @Inject
    public ListPoliciesAction(IamClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.LIST_POLICIES;
    }

    @Override
    public TaskResult execute(Context context, ListPoliciesParams input) throws Exception {
        try (var client = createClient(input)) {

            var policies = client.listPoliciesPaginator(ListPoliciesRequest.builder().build()).stream()
                    .flatMap(p -> p.policies().stream())
                    .map(AwsTaskUtils::serialize)
                    .toList();

            return TaskResult.success()
                    .value("policies", policies);
        } catch (IamException e) {
            log.error("❌ Failed to list IAM policies: {}", e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }
}
