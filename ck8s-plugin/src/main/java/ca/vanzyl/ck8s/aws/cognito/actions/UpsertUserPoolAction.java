package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceNotFoundException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolType;

import javax.inject.Inject;

public class UpsertUserPoolAction extends CognitoTaskAction<CognitoTaskParams.CreateUserPoolParams> {

    private static final Logger log = LoggerFactory.getLogger(UpsertUserPoolAction.class);

    private static final int MAX_RESULTS = 50;

    @Inject
    public UpsertUserPoolAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public CognitoTaskAction.Action action() {
        return Action.UPSERT_USER_POOL;
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateUserPoolParams input) throws Exception {
        var poolName = input.poolName();
        var usernameAttributes = input.usernameAttributes();
        var usernameConfiguration = input.usernameConfiguration();
        var policy = input.policy();
        var adminCreateUserConfig = input.adminCreateUserConfig();
        var emailConfiguration = input.emailConfiguration();
        var schema = input.schema();
        var tags = input.tags();

        try (var client = createClient(input)) {
            var poolId = finPoolIdByName(client, poolName);
            if (poolId == null) {
                log.info("User pool '{}' does not exists. Creating it...", poolName);

                var response = client.createUserPool(r -> r.poolName(poolName)
                        .usernameAttributes(usernameAttributes)
                        .usernameConfiguration(usernameConfiguration)
                        .policies(policy)
                        .adminCreateUserConfig(adminCreateUserConfig)
                        .emailConfiguration(emailConfiguration)
                        .schema(schema)
                        .userPoolTags(tags)
                );

                log.info("✅ User pool '{}' created, id: '{}'", poolName, response.userPool().id());

                return TaskResult.success()
                        .value("id", response.userPool().id());
            } else {
                log.info("User pool '{}' exists. Updating it...", poolName);

                client.updateUserPool(r -> r.userPoolId(poolId)
                        .poolName(poolName)
                        .policies(policy)
                        .adminCreateUserConfig(adminCreateUserConfig)
                        .emailConfiguration(emailConfiguration)
                        .userPoolTags(tags)
                );

                log.info("✅ User pool '{}' updated, id: '{}'", poolName, poolId);

                return TaskResult.success()
                        .value("id", poolId);
            }
        } catch (CognitoIdentityProviderException e) {
            log.error("❌ Failed to upsert user pool '{}': {}", poolName, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }

    public static String finPoolIdByName(CognitoIdentityProviderClient client, String poolName) {
        var userPools = client.listUserPoolsPaginator(
                        ListUserPoolsRequest.builder()
                                .maxResults(MAX_RESULTS)
                                .build()).stream()
                .flatMap(response -> response.userPools().stream())
                .filter(u -> poolName.equals(u.name()))
                .toList();

        if (userPools.size() > 1) {
            log.error("❌ Multiple user pools ({}) found for name: '{}'", userPools.size(), poolName);
            log.error("Pools: {}", userPools);
            throw new RuntimeException("Multiple user pools found for name: " + poolName);
        } else if (userPools.isEmpty()) {
            return null;
        }
        return userPools.get(0).id();
    }

    public static UserPoolType getUserPool(CognitoIdentityProviderClient client, String poolId) {
        try {
            return client.describeUserPool(r -> r.userPoolId(poolId)).userPool();
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }
}
