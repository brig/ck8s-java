package ca.vanzyl.ck8s.aws.s3;

import ca.vanzyl.ck8s.aws.AwsTaskUtils;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.PublicAccessBlockConfiguration;
import software.amazon.awssdk.services.s3.model.Tag;

import java.util.List;
import java.util.Map;

import static ca.vanzyl.ck8s.aws.s3.S3TaskParams.*;

public final class VariablesS3TaskParams {

    private static final String PROFILE_KEY = "profile";
    private static final String REGION_KEY = "region";
    private static final String DEBUG_KEY = "debug";

    private static final String BUCKET_KEY = "bucket";
    private static final String TAGS_KEY = "tags";

    public static CreateBucketParams createBucket(Context context, Variables variables) {
        return new CreateBucketParams(
                baseParams(context, variables),
                assertBucketName(variables),
                configuration(variables),
                publicAccessBlock(variables),
                versioningEnabled(variables)

        );
    }

    public static TagBucketParams tagBucket(Context context, Variables variables) {
        return new TagBucketParams(
                baseParams(context, variables),
                assertBucketName(variables),
                tags(variables)
        );
    }

    private static String assertBucketName(Variables variables) {
        return variables.assertString(BUCKET_KEY);
    }

    static CreateBucketConfiguration configuration(Variables input) {
        var m = input.<String, Object>getMap("configuration", Map.of());
        if (m.isEmpty()) {
            return null;
        }

        return AwsTaskUtils.deserialize(m, CreateBucketConfiguration.serializableBuilderClass()).build();
    }

    private static boolean versioningEnabled(Variables variables) {
        // Returns true if "versioning" is set to `true`
        return variables.getBoolean("versioning", false);
    }

    static PublicAccessBlockConfiguration publicAccessBlock(Variables input) {
        var m = input.<String, Object>getMap("publicAccessBlock", Map.of());
        if (m.isEmpty()) {
            return null;
        }

        return AwsTaskUtils.deserialize(m, PublicAccessBlockConfiguration.serializableBuilderClass()).build();
    }

    private static BaseParams baseParams(Context context, Variables variables) {
        //{ todo: backward compat
        var region = AwsTaskUtils.assertRegion(context, variables);
        if (region != null) {
            var profile = AwsTaskUtils.getProfile(context);
            return new BaseParams(profile, region, assertDebug(variables, context.processConfiguration().debug()));
        }
        //}

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

    private static List<Tag> tags(Variables variables) {
        return variables.getMap(TAGS_KEY, Map.of()).entrySet().stream()
                .map(t -> Tag.builder()
                        .key(String.valueOf(t.getKey()))
                        .value(String.valueOf(t.getValue()))
                        .build())
                .toList();
    }
}
