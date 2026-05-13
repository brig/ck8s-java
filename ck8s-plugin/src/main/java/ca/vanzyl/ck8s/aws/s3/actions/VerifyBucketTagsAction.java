package ca.vanzyl.ck8s.aws.s3.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.s3.S3ClientFactory;
import ca.vanzyl.ck8s.aws.s3.S3TaskAction;
import ca.vanzyl.ck8s.aws.s3.S3TaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.Tag;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static ca.vanzyl.ck8s.aws.s3.actions.CreateBucketAction.doesBucketExist;
import static ca.vanzyl.ck8s.aws.s3.state.S3BucketLoader.getBucketTagging;
import static ca.vanzyl.ck8s.utils.VerifyUtils.verifyAttribute;

public class VerifyBucketTagsAction extends S3TaskAction<S3TaskParams.TagBucketParams> {

    private final static Logger log = LoggerFactory.getLogger(VerifyBucketTagsAction.class);

    @Inject
    public VerifyBucketTagsAction(S3ClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.VERIFY_BUCKET_TAGS;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, S3TaskParams.TagBucketParams input) throws Exception {
        var bucket = input.bucket();
        var tags = input.tags();

        try (var client = createClient(input)) {
            var exists = doesBucketExist(client, bucket);
            if (!exists) {
                log.error("❌ Bucket '{}' does not exists", bucket);
                return TaskResult.fail("Bucket does not exist");
            }

            var existingTags = getBucketTagging(client, bucket);

            var existingTagsMap = tagsAsMap(existingTags);
            var newTagsAsMap = tagsAsMap(tags);

            // ignore tags diff
            verifyAttribute("Tags", existingTagsMap, newTagsAsMap);

            return TaskResult.success();
        } catch (S3Exception e) {
            log.error("❌ Failed to verify S3 bucket '{}' tags: {}", bucket, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to verify S3 bucket tags: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    private static Map<String, String> tagsAsMap(List<Tag> tags) {
        return tags.stream()
                .collect(Collectors.toMap(Tag::key, Tag::value));
    }
}
