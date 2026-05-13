package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.actions.CreateUserPoolUserAction;
import software.amazon.awssdk.regions.Region;

public class UserPoolUserLoader extends AbstractCognitoEntityLoader<UserPoolUserKey, UserPoolUser> {

    public UserPoolUserLoader(CognitoClientFactory clientFactory, String profile, Region region) {
        super(clientFactory, profile, region);
    }

    @Override
    public UserPoolUser load(UserPoolUserKey key) {
        try (var client = createClient()) {
            var userPoolClient = CreateUserPoolUserAction.getUser(client, key.poolId(), key.username());
            if (userPoolClient == null) {
                return null;
            }

            return new UserPoolUser(key.poolId(), key.username());
        }
    }
}
