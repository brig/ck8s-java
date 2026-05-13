package ca.vanzyl.ck8s.aws.efs;

import ca.vanzyl.ck8s.aws.AwsTaskUtils;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.efs.model.RootDirectory;
import software.amazon.awssdk.services.efs.model.Tag;

import java.util.List;
import java.util.Map;

import static ca.vanzyl.ck8s.aws.efs.EfsTaskParams.*;

public final class VariablesEfsTaskParams {

    private static final String PROFILE_KEY = "profile";
    private static final String REGION_KEY = "region";
    private static final String DEBUG_KEY = "debug";

    private static final String EFS_ID_KEY = "efsId";

    public static FindAccessPointParams findAccessPointParams(Context context, Variables variables) {
        return new FindAccessPointParams(
                baseParams(context, variables),
                assertEfsId(variables),
                variables.assertString("name")
        );
    }

    public static CreateAccessPointParams createAccessPointParams(Context context, Variables variables) {
        return new CreateAccessPointParams(
                baseParams(context, variables),
                assertEfsId(variables),
                variables.assertString("name"),
                assertRootDirectory(variables),
                tags(variables)
        );
    }

    public static DeleteAccessPointParams deleteAccessPointParams(Context context, Variables variables) {
        return new DeleteAccessPointParams(
                baseParams(context, variables),
                assertEfsId(variables),
                variables.assertString("name")
        );
    }

    public static FindFileSystemParams findFileSystemParams(Context context, Variables variables) {
        return new FindFileSystemParams(
                baseParams(context, variables),
                variables.assertString("name")
        );
    }

    private static RootDirectory assertRootDirectory(Variables variables) {
        var attributes = variables.<String, Object>assertMap("rootDirectory");
        return AwsTaskUtils.deserialize(attributes, RootDirectory.serializableBuilderClass()).build();
    }

    private static List<Tag> tags(Variables variables) {
        return variables.getMap("tags", Map.of()).entrySet().stream()
                .map(t -> Tag.builder()
                        .key(String.valueOf(t.getKey()))
                        .value(String.valueOf(t.getValue()))
                        .build())
                .toList();
    }

    private static String assertEfsId(Variables variables) {
        return variables.assertString(EFS_ID_KEY);
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
