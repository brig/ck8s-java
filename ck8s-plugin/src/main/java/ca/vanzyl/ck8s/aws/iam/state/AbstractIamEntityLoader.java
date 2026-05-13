package ca.vanzyl.ck8s.aws.iam.state;

import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.state.Entity;
import ca.vanzyl.ck8s.state.EntityKey;
import ca.vanzyl.ck8s.state.EntityLoader;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;

public abstract class AbstractIamEntityLoader<K extends EntityKey<E>, E extends Entity> implements EntityLoader<K, E> {

    private final IamClientFactory clientFactory;
    private final String profile;
    private final Region region;

    public AbstractIamEntityLoader(IamClientFactory clientFactory, String profile, Region region) {
        this.clientFactory = clientFactory;
        this.region = region;
        this.profile = profile;
    }

    protected IamClient createClient() {
        return clientFactory.create(profile, region);
    }
}
