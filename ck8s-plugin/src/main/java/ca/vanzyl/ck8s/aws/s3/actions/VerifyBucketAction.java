package ca.vanzyl.ck8s.aws.s3.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.s3.S3ClientFactory;
import ca.vanzyl.ck8s.aws.s3.S3TaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.model.S3Exception;

import javax.inject.Inject;
import java.util.Set;

import static ca.vanzyl.ck8s.aws.s3.S3TaskParams.CreateBucketParams;

public class VerifyBucketAction extends S3TaskAction<CreateBucketParams> {

    private final static Logger log = LoggerFactory.getLogger(VerifyBucketAction.class);

    @Inject
    public VerifyBucketAction(S3ClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.VERIFY_BUCKET;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, CreateBucketParams input) throws Exception {
        var bucket = input.bucket();
        var region = input.baseParams().region();
        var configuration = input.configuration();
        var publicAccessBlock = input.publicAccessBlock();

        try (var client = createClient(input)) {
            if (CreateBucketAction.doesBucketExist(client, bucket)) {
                log.info("✅ Bucket '{}' in '{}' exists...", bucket, region);
            } else {
                log.warn("❌ Bucket '{}' in '{}' does not exist...", bucket, region);
            }

            return TaskResult.success();
        } catch (S3Exception e) {
            log.error("❌ Failed to verify bucket '{}': {}", bucket, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        } catch (RuntimeException e) {
            return TaskResult.fail(e);
        }
    }
}
