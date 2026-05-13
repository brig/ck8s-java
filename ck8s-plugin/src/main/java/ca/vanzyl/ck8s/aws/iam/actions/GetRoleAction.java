package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.AwsTaskUtils;
import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.GetRoleRequest;
import software.amazon.awssdk.services.iam.model.IamException;
import software.amazon.awssdk.services.iam.model.NoSuchEntityException;
import software.amazon.awssdk.services.iam.model.Role;

import javax.inject.Inject;
import java.util.Set;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.GetRoleParams;

public class GetRoleAction extends IamTaskAction<GetRoleParams> {

    private final static Logger log = LoggerFactory.getLogger(GetRoleAction.class);

    @Inject
    public GetRoleAction(IamClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.GET_ROLE;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, GetRoleParams input) throws Exception {
        var roleName = input.roleName();

        try (var client = createClient(input)) {
            var roleOrNull = getRole(client, roleName);
            return TaskResult.success()
                    .value("role", AwsTaskUtils.serialize(roleOrNull));
        } catch (IamException e) {
            log.error("❌ Failed to get role '{}': {}", roleName, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }

    public static Role getRole(IamClient client, String roleName) throws IamException {
        try {
            GetRoleRequest request = GetRoleRequest.builder().roleName(roleName).build();
            return client.getRole(request).role();
        } catch (NoSuchEntityException e) {
            return null;
        }
    }
}
