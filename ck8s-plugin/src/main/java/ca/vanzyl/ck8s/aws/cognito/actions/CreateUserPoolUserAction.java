package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.inject.Inject;

public class CreateUserPoolUserAction extends CognitoTaskAction<CognitoTaskParams.CreateUserPoolUserParams> {

    private static final Logger log = LoggerFactory.getLogger(CreateUserPoolUserAction.class);

    @Inject
    public CreateUserPoolUserAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.CREATE_USER_POOL_USER;
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateUserPoolUserParams input) throws Exception {
        var poolId = input.poolId();
        var username = input.username();

        try (var client = createClient(input)) {
            var existingUser = getUser(client, poolId, username);
            if (existingUser == null) {
                log.info("User '{}' in user pool '{}' does not exists. Creating it...", username, poolId);

                client.adminCreateUser(r -> r.userPoolId(poolId)
                        .username(username)
                        .userAttributes(AttributeType.builder()
                                .name("email")
                                .value(username)
                                .build())
                        .messageAction(MessageActionType.SUPPRESS)
                        .build());

                // Mark email as verified
                client.adminUpdateUserAttributes(r -> r.userPoolId(poolId)
                        .username(username)
                        .userAttributes(AttributeType.builder()
                                .name("email_verified")
                                .value("true")
                                .build())
                        .build());

                // Set permanent password
                client.adminSetUserPassword(r -> r.userPoolId(poolId)
                        .username(username)
                        .password(input.password())
                        .permanent(true)
                        .build());

                log.info("✅ User '{}' in user pool '{}' created", username, poolId);
            } else {
                log.info("✅ User '{}' in user pool '{}' exists. Do nothing...", username, poolId);
            }

            return TaskResult.success();
        } catch (CognitoIdentityProviderException e) {
            log.error("❌ Failed to upsert user pool '{}' user '{}': {}", poolId, username, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }

    public static AdminGetUserResponse getUser(CognitoIdentityProviderClient client, String poolId, String username) {
        try {
            return client.adminGetUser(r -> r.userPoolId(poolId)
                    .username(username));
        } catch (ResourceNotFoundException | UserNotFoundException e) {
            return null;
        }
    }
}
