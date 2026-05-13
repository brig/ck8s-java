package ca.vanzyl.ck8s.aws.s3.state;

import ca.vanzyl.ck8s.aws.s3.S3ClientFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetPublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.Tag;

import java.util.List;
import java.util.stream.Collectors;

import static ca.vanzyl.ck8s.aws.s3.actions.CreateBucketAction.doesBucketExist;

public class S3BucketLoader extends AbstractS3EntityLoader<S3BucketKey, S3Bucket> {

    public S3BucketLoader(S3ClientFactory clientFactory, String profile, Region region) {
        super(clientFactory, profile, region);
    }

    @Override
    public S3Bucket load(S3BucketKey key) {
        var bucketName = key.bucketName();

        try (var client = createClient()) {
            var exists = doesBucketExist(client, bucketName);
            if (!exists) {
                return null;
            }

            var publicAccessBlock = client.getPublicAccessBlock(GetPublicAccessBlockRequest.builder()
                    .bucket(bucketName)
                    .build()).publicAccessBlockConfiguration();

            var tags = getBucketTagging(client, bucketName);

            return S3Bucket.builder()
                    .bucketName(bucketName)
                    .publicAccessBlock(publicAccessBlock)
                    .tags(tags.stream().collect(Collectors.toMap(Tag::key, Tag::value)))
                    .build();
        }
    }

    public static List<Tag> getBucketTagging(S3Client client, String bucketName) {
        try {
            return client.getBucketTagging(
                    GetBucketTaggingRequest.builder()
                            .bucket(bucketName)
                            .build()).tagSet();
        } catch (S3Exception e) {
            if ("NoSuchTagSet".equals(e.awsErrorDetails().errorCode())) {
                return List.of();
            }
            throw e;
        }
    }
}
