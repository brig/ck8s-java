package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.google.inject.Inject;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolDescriptionType;

public class DeleteUserPoolAction extends CognitoTaskAction<CognitoTaskParams.DeleteUserPoolParams> {

    private static final Logger log = LoggerFactory.getLogger(DeleteUserPoolAction.class);

    @Inject
    public DeleteUserPoolAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.DELETE_USER_POOL;
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.DeleteUserPoolParams input) throws Exception {
        var poolName = input.poolName();
        try (var client = createClient(input)) {

            var poolIds = client.listUserPoolsPaginator(r -> r.maxResults(input.maxResults())).stream()
                    .flatMap(r -> r.userPools().stream())
                    .filter(up -> poolName.equals(up.name()))
                    .map(UserPoolDescriptionType::id)
                    .toList();

            log.info("Deleting user pools: '{}'", poolIds);

            DeleteUserPoolsAction.deleteUserPools(client, poolIds);

            return TaskResult.success();
        } catch (CognitoIdentityProviderException e) {
            log.error("❌ Failed to delete user pool: {}", e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }
}
