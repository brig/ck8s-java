package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.actions.UpsertUserPoolAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import software.amazon.awssdk.regions.Region;

public class CognitoUserPoolIdLoader extends AbstractCognitoEntityLoader<UserPoolIdKey, UserPoolId> {

    public CognitoUserPoolIdLoader(Context context, CognitoClientFactory clientFactory, String profile, Region region) {
        super(context, clientFactory, profile, region);
    }

    @Override
    public UserPoolId load(UserPoolIdKey key) {
        var poolName = key.poolName();

        try (var client = createClient()) {
            var id = UpsertUserPoolAction.finPoolIdByName(client, poolName);
            if (id == null) {
                return null;
            }

            return new UserPoolId(id, poolName);
        }
    }
}
