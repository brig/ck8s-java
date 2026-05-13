package ca.vanzyl.ck8s.aws.s3.state;

import ca.vanzyl.ck8s.aws.s3.S3ClientFactory;
import ca.vanzyl.ck8s.aws.s3.S3TaskParams;
import ca.vanzyl.ck8s.state.EntityState;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class S3State {

    private final S3ClientFactory clientFactory;
    private final EntityState state;

    @Inject
    public S3State(S3ClientFactory clientFactory, EntityState state) {
        this.clientFactory = clientFactory;
        this.state = state;
    }

    public S3Bucket bucket(S3TaskParams.BaseParams baseParams, String bucket) {
        return state.getOrLoad(new S3BucketKey(bucket),
                new S3BucketLoader(clientFactory, baseParams.profile(), baseParams.region()));
    }

    public void put(S3Bucket bucket) {
        state.put(new S3BucketKey(bucket.bucketName()), bucket);
    }
}
