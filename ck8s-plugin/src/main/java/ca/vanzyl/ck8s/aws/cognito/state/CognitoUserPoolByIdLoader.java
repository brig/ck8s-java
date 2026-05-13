package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.actions.UpsertUserPoolAction;
import software.amazon.awssdk.regions.Region;

public class CognitoUserPoolByIdLoader extends AbstractCognitoEntityLoader<UserPoolKey, UserPool> {

    public CognitoUserPoolByIdLoader(CognitoClientFactory clientFactory, String profile, Region region) {
        super(clientFactory, profile, region);
    }

    @Override
    public UserPool load(UserPoolKey key) {
        var poolId = key.poolId();

        try (var client = createClient()) {
            var pool = UpsertUserPoolAction.getUserPool(client, poolId);
            if (pool == null) {
                return null;
            }

            return new UserPool(pool.id(), pool.name());
        }
    }
}
