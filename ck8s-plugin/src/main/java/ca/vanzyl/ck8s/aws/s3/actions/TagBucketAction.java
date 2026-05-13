package ca.vanzyl.ck8s.aws.s3.actions;

import ca.vanzyl.ck8s.aws.s3.S3ClientFactory;
import ca.vanzyl.ck8s.aws.s3.S3TaskAction;
import ca.vanzyl.ck8s.aws.s3.S3TaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.model.PutBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.Tagging;

import javax.inject.Inject;

public class TagBucketAction extends S3TaskAction<S3TaskParams.TagBucketParams> {

    private final static Logger log = LoggerFactory.getLogger(TagBucketAction.class);

    @Inject
    public TagBucketAction(S3ClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.TAG_BUCKET;
    }

    @Override
    public TaskResult execute(Context context, S3TaskParams.TagBucketParams input) throws Exception {
        var bucket = input.bucket();
        var tags = input.tags();

        try (var client = createClient(input)) {
            client.putBucketTagging(PutBucketTaggingRequest.builder()
                    .bucket(bucket)
                    .tagging(Tagging.builder().tagSet(tags).build())
                    .build());

            log.info("✅ Tags for bucket '{}' updated successfully", bucket);

            return TaskResult.success();
        } catch (S3Exception e) {
            log.error("❌ Failed to tag S3 bucket '{}': {}", bucket, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to tag S3 bucket: " + e.awsErrorDetails().errorMessage(), e);
        }
    }
}
