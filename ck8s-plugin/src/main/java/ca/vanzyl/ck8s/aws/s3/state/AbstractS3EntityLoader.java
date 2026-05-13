package ca.vanzyl.ck8s.aws.s3.state;

import ca.vanzyl.ck8s.aws.s3.S3ClientFactory;
import ca.vanzyl.ck8s.state.Entity;
import ca.vanzyl.ck8s.state.EntityKey;
import ca.vanzyl.ck8s.state.EntityLoader;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public abstract class AbstractS3EntityLoader<K extends EntityKey<E>, E extends Entity> implements EntityLoader<K, E> {

    private final S3ClientFactory clientFactory;
    private final String profile;
    private final Region region;

    public AbstractS3EntityLoader(S3ClientFactory clientFactory, String profile, Region region) {
        this.clientFactory = clientFactory;
        this.region = region;
        this.profile = profile;
    }

    protected S3Client createClient() {
        return clientFactory.create(profile, region);
    }
}
