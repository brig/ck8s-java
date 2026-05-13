package ca.vanzyl.ck8s.aws.s3;

import ca.vanzyl.ck8s.actions.ActionInput;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;

public interface S3TaskParams extends ActionInput {

    BaseParams baseParams();

    record BaseParams(String profile, Region region, boolean debug) {
    }

    record CreateBucketParams(
            S3TaskParams.BaseParams baseParams,
            String bucket,
            CreateBucketConfiguration configuration,
            PublicAccessBlockConfiguration publicAccessBlock,
            boolean versioning

    ) implements S3TaskParams {
    }

    record TagBucketParams(
            BaseParams baseParams,
            String bucket,
            List<Tag> tags
    ) implements S3TaskParams {
    }
}
