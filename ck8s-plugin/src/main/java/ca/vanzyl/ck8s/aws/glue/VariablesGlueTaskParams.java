package ca.vanzyl.ck8s.aws.glue;

import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import software.amazon.awssdk.regions.Region;

import static ca.vanzyl.ck8s.aws.glue.GlueTaskParams.BaseParams;
import static ca.vanzyl.ck8s.aws.glue.GlueTaskParams.ExistsParams;

public final class VariablesGlueTaskParams {

    private static final String PROFILE_KEY = "profile";
    private static final String REGION_KEY = "region";
    private static final String DEBUG_KEY = "debug";

    private static final String DATABASE_NAME_KEY = "database";

    public static ExistsParams exists(Context context, Variables variables) {
        return new ExistsParams(
                baseParams(context, variables),
                assertDatabaseName(variables)
        );
    }

    private static String assertDatabaseName(Variables variables) {
        return variables.assertString(DATABASE_NAME_KEY);
    }

    private static BaseParams baseParams(Context context, Variables variables) {
        return new BaseParams(profile(variables), assertRegion(variables), assertDebug(variables, context.processConfiguration().debug()));
    }

    private static String profile(Variables variables) {
        return variables.getString(PROFILE_KEY);
    }

    private static Region assertRegion(Variables variables) {
        return Region.of(variables.assertString(REGION_KEY));
    }

    private static boolean assertDebug(Variables variables, boolean defaultValue) {
        return variables.getBoolean(DEBUG_KEY, defaultValue);
    }
}
