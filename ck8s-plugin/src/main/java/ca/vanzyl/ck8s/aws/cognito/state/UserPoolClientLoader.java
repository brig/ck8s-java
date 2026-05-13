package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.actions.UpsertUserPoolClientAction;
import software.amazon.awssdk.regions.Region;

public class UserPoolClientLoader extends AbstractCognitoEntityLoader<UserPoolClientKey, UserPoolClient> {

    public UserPoolClientLoader(CognitoClientFactory clientFactory, String profile, Region region) {
        super(clientFactory, profile, region);
    }

    @Override
    public UserPoolClient load(UserPoolClientKey key) {
        try (var client = createClient()) {
            var userPoolClient = UpsertUserPoolClientAction.getClient(client, key.poolId(), key.clientName());
            if (userPoolClient == null) {
                return null;
            }

            return new UserPoolClient(key.poolId(), userPoolClient.clientId(), key.clientName());
        }
    }
}
