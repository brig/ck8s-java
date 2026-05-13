package ca.vanzyl.ck8s.aws.efs;

import ca.vanzyl.ck8s.actions.ActionInput;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.efs.model.RootDirectory;
import software.amazon.awssdk.services.efs.model.Tag;

import java.util.List;

public interface EfsTaskParams extends ActionInput {

    BaseParams baseParams();

    record BaseParams(String profile, Region region, boolean debug) {
    }

    record CreateAccessPointParams(
            EfsTaskParams.BaseParams baseParams,
            String efsId,
            String name,
            RootDirectory rootDirectory,
            List<Tag> tags
    ) implements EfsTaskParams {
    }

    record DeleteAccessPointParams(
            EfsTaskParams.BaseParams baseParams,
            String efsId,
            String name
    ) implements EfsTaskParams {
    }

    record FindAccessPointParams(
            EfsTaskParams.BaseParams baseParams,
            String efsId,
            String name
    ) implements EfsTaskParams {
    }

    record FindFileSystemParams(
            EfsTaskParams.BaseParams baseParams,
            String name
    ) implements EfsTaskParams {
    }
}
