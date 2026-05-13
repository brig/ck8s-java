package ca.vanzyl.ck8s.aws.s3.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.s3.S3ClientFactory;
import ca.vanzyl.ck8s.aws.s3.S3TaskAction;
import ca.vanzyl.ck8s.aws.s3.state.S3Bucket;
import ca.vanzyl.ck8s.aws.s3.state.S3State;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.model.Tag;

import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;

import static ca.vanzyl.ck8s.aws.s3.S3TaskParams.TagBucketParams;

public class TagBucketPreviewAction extends S3TaskAction<TagBucketParams> {

    private final static Logger log = LoggerFactory.getLogger(TagBucketPreviewAction.class);

    private final S3State state;

    @Inject
    public TagBucketPreviewAction(S3ClientFactory clientFactory, S3State state) {
        super(clientFactory);
        this.state = state;
    }

    @Override
    public Action action() {
        return Action.TAG_BUCKET;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.PREVIEW);
    }

    @Override
    public TaskResult execute(Context context, TagBucketParams input) throws Exception {
        var bucket = input.bucket();
        var region = input.baseParams().region();

        // just to load current
        var stateBucket = state.bucket(input.baseParams(), bucket);
        if (stateBucket == null) {
            return TaskResult.fail("Bucket '" + bucket + "' not found");
        } else {
            log.info("[PREVIEW] Bucket '{}' in '{}' exists. Updating tags...", bucket, region);
            state.put(S3Bucket.builder().from(stateBucket)
                    .tags(input.tags().stream().collect(Collectors.toMap(Tag::key, Tag::value)))
                    .build());
        }

        return TaskResult.success();
    }
}
