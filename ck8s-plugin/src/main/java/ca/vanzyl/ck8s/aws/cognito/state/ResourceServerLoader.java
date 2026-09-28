package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.actions.UpsertResourceServerAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import software.amazon.awssdk.regions.Region;

public class ResourceServerLoader extends AbstractCognitoEntityLoader<ResourceServerKey, ResourceServer> {

    public ResourceServerLoader(Context context, CognitoClientFactory clientFactory, String profile, Region region) {
        super(context, clientFactory, profile, region);
    }

    @Override
    public ResourceServer load(ResourceServerKey key) {
        try (var client = createClient()) {
            var resourceServer = UpsertResourceServerAction.getResourceServer(client, key.poolId(), key.identifier());
            if (resourceServer == null) {
                return null;
            }

            return new ResourceServer(key.poolId(), key.identifier(), resourceServer.name());
        }
    }
}
