package ca.vanzyl.ck8s.aws.glue;

import ca.vanzyl.ck8s.actions.ActionInput;
import software.amazon.awssdk.regions.Region;

public interface GlueTaskParams extends ActionInput {

    BaseParams baseParams();

    record BaseParams(String profile, Region region, boolean debug) {
    }

    record ExistsParams(
            BaseParams baseParams,
            String databaseName
    ) implements GlueTaskParams {
    }

}
