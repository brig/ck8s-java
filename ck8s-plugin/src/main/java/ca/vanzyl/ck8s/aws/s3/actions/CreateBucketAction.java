package ca.vanzyl.ck8s.aws.s3.actions;

import ca.vanzyl.ck8s.aws.s3.S3ClientFactory;
import ca.vanzyl.ck8s.aws.s3.S3TaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.inject.Inject;

import static ca.vanzyl.ck8s.aws.s3.S3TaskParams.CreateBucketParams;

public class CreateBucketAction extends S3TaskAction<CreateBucketParams> {

    private final static Logger log = LoggerFactory.getLogger(CreateBucketAction.class);

    @Inject
    public CreateBucketAction(S3ClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.CREATE_BUCKET;
    }

    @Override
    public TaskResult execute(Context context, CreateBucketParams input) throws Exception {
        var bucket = input.bucket();
        var region = input.baseParams().region();
        var configuration = input.configuration();
        var publicAccessBlock = input.publicAccessBlock();
        var versioning = input.versioning();

        dumpInput(input);

        try (var client = createClient(input)) {
            if (doesBucketExist(client, bucket)) {
                log.info("Bucket '{}' in '{}' exists. Updating it...", bucket, region);
            } else {
                log.info("Bucket '{}' in '{}' does not exists. Creating it...", bucket, region);
                createBucket(client, bucket, configuration);
            }

            ensurePublicAccessBlock(client, bucket, publicAccessBlock);
            if (versioning) {
                setBucketVersioning(client, bucket, versioning);
            }

            return TaskResult.success();
        } catch (S3Exception e) {
            log.error("❌ Failed to create bucket '{}': {}", bucket, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        } catch (RuntimeException e) {
            return TaskResult.fail(e);
        }
    }

    public static boolean doesBucketExist(S3Client client, String bucketName) {
        try {
            client.getBucketAcl(r -> r.bucket(bucketName));
            return true;
        } catch (S3Exception e) {
            // A redirect error or an AccessDenied exception means the bucket exists but it's not in this region
            // or we don't have permissions to it.
            if ((e.statusCode() == 301) || "AccessDenied".equals(e.awsErrorDetails().errorCode())) {
                return true;
            }
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    private static void createBucket(S3Client client, String bucketName, CreateBucketConfiguration configuration) {
        try {
            client.createBucket(
                    CreateBucketRequest.builder()
                            .bucket(bucketName)
                            .createBucketConfiguration(configuration)
                            .build());

            log.info("✅ Bucket created: '{}'", bucketName);
        } catch (S3Exception e) {
            log.error("❌ Failed to create bucket '{}': {}", bucketName, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to create bucket: " + e.awsErrorDetails().errorMessage());
        }
    }

    private static void ensurePublicAccessBlock(S3Client client, String bucketName, PublicAccessBlockConfiguration required) {
        try {
            var current = client.getPublicAccessBlock(GetPublicAccessBlockRequest.builder()
                    .bucket(bucketName)
                    .build()).publicAccessBlockConfiguration();

            if (current == null && required == null || (current != null && current.equals(required))) {
                log.info("✅ Public access block already set correctly.");
                return;
            }

            log.info("Public access block is not set to the required configuration. Current: ${}, required: {}, Attempting to set it...", current, required);

            setPublicAccessBlock(client, bucketName, required);

            log.info("✅ Public access block updated.");
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.info("Public access block is not set. Setting it now...");
                setPublicAccessBlock(client, bucketName, required);
            } else {
                throw e;
            }
        }
    }

    private static void setPublicAccessBlock(S3Client client, String bucketName, PublicAccessBlockConfiguration config) {
        try {
            if (config == null) {
                client.deletePublicAccessBlock(DeletePublicAccessBlockRequest.builder().bucket(bucketName).build());

                log.info("Public access block removed from bucket: {}", bucketName);
            } else {
                client.putPublicAccessBlock(PutPublicAccessBlockRequest.builder()
                        .bucket(bucketName)
                        .publicAccessBlockConfiguration(config)
                        .build());
                log.info("Public access block {} set for bucket: {}", config, bucketName);
            }
        } catch (S3Exception e) {
            log.error("❌ Failed to set public access bloc '{}': {}", bucketName, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to set public access block: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    private static void setBucketVersioning(S3Client client, String bucketName, boolean enable) {
        try {
            BucketVersioningStatus status = enable ? BucketVersioningStatus.ENABLED : BucketVersioningStatus.SUSPENDED;
            client.putBucketVersioning(PutBucketVersioningRequest.builder()
                    .bucket(bucketName)
                    .versioningConfiguration(VersioningConfiguration.builder()
                            .status(status)
                            .build())
                    .build());
            log.info("✅ Bucket versioning set to '{}' for bucket: {}", status, bucketName);
        } catch (S3Exception e) {
            log.error("❌ Failed to set bucket versioning for '{}': {}", bucketName, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to set bucket versioning: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    public static void dumpInput(CreateBucketParams input) {
        if (input.baseParams().debug()) {
            log.info("Bucket:\n{}", input.bucket());
            log.info("Configuration:\n{}", input.configuration());
            log.info("Public Access Block Configuration:\n{}", input.publicAccessBlock());
        }
    }
}
