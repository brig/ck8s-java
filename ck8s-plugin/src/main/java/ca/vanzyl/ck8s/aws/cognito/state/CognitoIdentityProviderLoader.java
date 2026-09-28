package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.actions.UpsertIdentityProviderAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import software.amazon.awssdk.regions.Region;

public class CognitoIdentityProviderLoader extends AbstractCognitoEntityLoader<IdentityProviderKey, IdentityProvider> {

    public CognitoIdentityProviderLoader(Context context, CognitoClientFactory clientFactory, String profile, Region region) {
        super(context, clientFactory, profile, region);
    }

    @Override
    public IdentityProvider load(IdentityProviderKey key) {
        try (var client = createClient()) {
            var provider = UpsertIdentityProviderAction.findIdentityProviderByName(client, key.poolId(), key.providerName());
            if (provider == null) {
                return null;
            }

            return new IdentityProvider(key.poolId(), key.providerName());
        }
    }
}
