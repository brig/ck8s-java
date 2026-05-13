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

import javax.inject.Inject;
import java.util.Set;

import static ca.vanzyl.ck8s.aws.s3.S3TaskParams.CreateBucketParams;
import static ca.vanzyl.ck8s.aws.s3.actions.CreateBucketAction.dumpInput;

public class CreateBucketPreviewAction extends S3TaskAction<CreateBucketParams> {

    private final static Logger log = LoggerFactory.getLogger(CreateBucketPreviewAction.class);

    private final S3State state;

    @Inject
    public CreateBucketPreviewAction(S3ClientFactory clientFactory, S3State state) {
        super(clientFactory);
        this.state = state;
    }

    @Override
    public Action action() {
        return Action.CREATE_BUCKET;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.PREVIEW);
    }

    @Override
    public TaskResult execute(Context context, CreateBucketParams input) throws Exception {
        var bucket = input.bucket();
        var region = input.baseParams().region();
        var publicAccessBlock = input.publicAccessBlock();

        dumpInput(input);

        // just to load current
        var stateBucket = state.bucket(input.baseParams(), bucket);
        if (stateBucket == null) {
            log.info("[PREVIEW] Bucket '{}' in '{}' does not exists. Creating it...", bucket, region);
            state.put(S3Bucket.builder()
                    .bucketName(bucket)
                    .publicAccessBlock(publicAccessBlock)
                    .build());
        } else {
            log.info("[PREVIEW] Bucket '{}' in '{}' exists. Updating it...", bucket, region);
            state.put(S3Bucket.builder().from(stateBucket)
                    .bucketName(bucket)
                    .publicAccessBlock(publicAccessBlock)
                    .build());
        }

        return TaskResult.success();
    }
}
