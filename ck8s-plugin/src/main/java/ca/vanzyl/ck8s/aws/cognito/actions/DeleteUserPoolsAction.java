package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.google.inject.Inject;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeleteUserPoolDomainRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeleteUserPoolRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolRequest;

import java.util.List;

public class DeleteUserPoolsAction extends CognitoTaskAction<CognitoTaskParams.DeleteUserPoolsParams> {

    private static final Logger log = LoggerFactory.getLogger(DeleteUserPoolsAction.class);

    public static void deleteUserPools(CognitoIdentityProviderClient client, List<String> ids) {
        for (var id : ids) {
            var poolResponse = client.describeUserPool(DescribeUserPoolRequest.builder()
                    .userPoolId(id)
                    .build());
            if (poolResponse != null && poolResponse.userPool().domain() != null) {
                log.info("Deleting user pool '{}' domain: '{}'", id, poolResponse.userPool().domain());

                client.deleteUserPoolDomain(DeleteUserPoolDomainRequest.builder()
                        .userPoolId(id)
                        .domain(poolResponse.userPool().domain())
                        .build());
            }
            log.info("Deleting user pool '{}'", id);
            client.deleteUserPool(DeleteUserPoolRequest.builder().userPoolId(id).build());
        }
    }

    @Inject
    public DeleteUserPoolsAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.DELETE_USER_POOLS;
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.DeleteUserPoolsParams input) throws Exception {
        var ids = input.ids();

        if (ids.isEmpty()) {
            log.info("No user pools provided, skipping delete");
            return TaskResult.success();
        }

        try (var client = createClient(input)) {

            deleteUserPools(client, ids);

            return TaskResult.success();
        } catch (CognitoIdentityProviderException e) {
            log.error("❌ Failed to delete user pools: {}", e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }
}
