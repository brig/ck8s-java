package ca.vanzyl.ck8s.aws.s3.state;

import ca.vanzyl.ck8s.state.EntityKey;

public record S3BucketKey (String bucketName) implements EntityKey<S3Bucket> {

    @Override
    public Class<S3Bucket> entityType() {
        return S3Bucket.class;
    }
}
