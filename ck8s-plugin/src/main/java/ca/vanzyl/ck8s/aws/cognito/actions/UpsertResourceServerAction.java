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
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceNotFoundException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceServerType;

import javax.inject.Inject;

public class UpsertResourceServerAction extends CognitoTaskAction<CognitoTaskParams.CreateResourceServerParams> {

    private static final Logger log = LoggerFactory.getLogger(UpsertResourceServerAction.class);

    public static ResourceServerType getResourceServer(CognitoIdentityProviderClient client, String userPoolId, String identifier) {
        try {
            return client.describeResourceServer(r -> r.userPoolId(userPoolId)
                    .identifier(identifier)).resourceServer();
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    @Inject
    public UpsertResourceServerAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.UPSERT_RESOURCE_SERVER;
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateResourceServerParams input) throws Exception {
        var poolId = input.poolId();
        var name = input.name();
        var identifier = input.identifier();
        var scopes = input.scopes();

        try (var client = createClient(input)) {
            var existingResourceServer = getResourceServer(client, poolId, identifier);
            if (existingResourceServer == null) {
                log.info("Resource server '{}' in pool '{}' does not exists. Creating it...", identifier, poolId);

                client.createResourceServer(r -> r.userPoolId(poolId)
                        .name(name)
                        .identifier(identifier)
                        .scopes(scopes));

                log.info("✅ Resource server '{}' created", identifier);
            } else {
                log.info("Resource server '{}' in pool '{}' exists. Updating it...", identifier, poolId);

                client.updateResourceServer(r -> r.userPoolId(poolId)
                        .name(name)
                        .identifier(identifier)
                        .scopes(scopes));

                log.info("✅ Resource server '{}' updated", identifier);
            }

            return TaskResult.success();
        } catch (CognitoIdentityProviderException e) {
            log.error("❌ Failed to create resource server: {}", e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }
}
