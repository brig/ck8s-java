package ca.vanzyl.ck8s.aws.rds;

import ca.vanzyl.ck8s.actions.ActionInput;
import software.amazon.awssdk.regions.Region;

public interface RdsTaskParams extends ActionInput {

    BaseParams baseParams();

    record BaseParams(String profile, Region region, boolean debug) {
    }

    record FetchEndpointParams(
            RdsTaskParams.BaseParams baseParams,
            String engine,
            String identifier
    ) implements RdsTaskParams {
    }
}
