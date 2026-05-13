package ca.vanzyl.ck8s.aws.cloudformation.state;

import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationClientFactory;
import ca.vanzyl.ck8s.state.Entity;
import ca.vanzyl.ck8s.state.EntityKey;
import ca.vanzyl.ck8s.state.EntityLoader;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

public abstract class AbstractCloudFormationEntityLoader<K extends EntityKey<E>, E extends Entity> implements EntityLoader<K, E> {

    private final CloudFormationClientFactory clientFactory;
    private final String profile;
    private final Region region;

    public AbstractCloudFormationEntityLoader(CloudFormationClientFactory clientFactory, String profile, Region region) {
        this.clientFactory = clientFactory;
        this.region = region;
        this.profile = profile;
    }

    protected CloudFormationClient createClient() {
        return clientFactory.create(profile, region);
    }
}
