package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.aws.AwsTaskUtils;
import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.model.IamException;
import software.amazon.awssdk.services.iam.model.ListRolesRequest;

import javax.inject.Inject;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.ListRolesParams;

public class ListRolesAction extends IamTaskAction<ListRolesParams> {

    private final static Logger log = LoggerFactory.getLogger(ListRolesAction.class);

    @Inject
    public ListRolesAction(IamClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.LIST_ROLES;
    }

    @Override
    public TaskResult execute(Context context, ListRolesParams input) throws Exception {
        try (var client = createClient(input)) {
            var roles = client.listRolesPaginator(ListRolesRequest.builder().build()).stream()
                    .flatMap(r -> r.roles().stream())
                    .map(AwsTaskUtils::serialize)
                    .toList();

            return TaskResult.success()
                    .value("roles", roles);
        } catch (IamException e) {
            log.error("❌ Failed to list IAM roles: {}", e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }
}
