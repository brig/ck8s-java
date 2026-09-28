package ca.vanzyl.ck8s.aws.cloudformation.state;

import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationClientFactory;
import ca.vanzyl.ck8s.state.Entity;
import ca.vanzyl.ck8s.state.EntityKey;
import ca.vanzyl.ck8s.state.EntityLoader;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

public abstract class AbstractCloudFormationEntityLoader<K extends EntityKey<E>, E extends Entity> implements EntityLoader<K, E> {

    private final Context context;
    private final CloudFormationClientFactory clientFactory;
    private final String profile;
    private final Region region;

    public AbstractCloudFormationEntityLoader(Context context, CloudFormationClientFactory clientFactory, String profile, Region region) {
        this.context = context;
        this.clientFactory = clientFactory;
        this.region = region;
        this.profile = profile;
    }

    protected CloudFormationClient createClient() {
        return clientFactory.create(context, profile, region);
    }
}
