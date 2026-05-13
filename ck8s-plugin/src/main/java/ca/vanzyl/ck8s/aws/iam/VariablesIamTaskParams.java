package ca.vanzyl.ck8s.aws.iam;

import ca.vanzyl.ck8s.aws.AwsTaskUtils;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.model.Tag;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.*;
import static ca.vanzyl.ck8s.utils.VariablesUtils.assertFile;

public final class VariablesIamTaskParams {

    private static final String PROFILE_KEY = "profile";
    private static final String REGION_KEY = "region";
    private static final String DEBUG_KEY = "debug";

    private static final String TRUST_POLICY_KEY = "trustPolicy";
    private static final String TRUST_POLICY_FILE_KEY = "trustPolicyFile";
    private static final String ROLE_NAME = "role";
    private static final String TAGS_KEY = "tags";
    private static final String POLICY_NAME_KEY = "policyName";
    private static final String POLICY_FILE_KEY = "policyFile";
    private static final String POLICY_ARN_KEY = "policyArn";
    private static final String DETACH_FROM_RESOURCES_KEY = "detachFromResources";

    public static CreateRoleParams createRole(Context context, Variables variables) {
        return new CreateRoleParams(
                baseParams(context, variables),
                assertRoleName(variables),
                assertTrustPolicy(context.workingDirectory(), variables),
                tags(variables)
        );
    }

    public static DeleteRoleParams deleteRole(Context context, Variables variables) {
        return new DeleteRoleParams(
                baseParams(context, variables),
                assertRoleName(variables)
        );
    }

    public static GetRoleParams getRole(Context context, Variables variables) {
        return new GetRoleParams(
                baseParams(context, variables),
                assertRoleName(variables)
        );
    }

    public static ListRolesParams listRoles(Context context, Variables variables) {
        return new ListRolesParams(baseParams(context, variables));
    }

    public static PutRolePolicyParams putRolePolicy(Context context, Variables variables) {
        return new PutRolePolicyParams(
                baseParams(context, variables),
                assertRoleName(variables),
                assertPolicyName(variables),
                assertPolicyDocument(context.workingDirectory(), variables)
        );
    }

    public static CreatePolicyParams createPolicy(Context context, Variables variables) {
        return new CreatePolicyParams(
                baseParams(context, variables),
                assertPolicyName(variables),
                assertPolicyArn(variables),
                assertPolicyDocument(context.workingDirectory(), variables)
        );
    }

    public static DeletePolicyParams deletePolicy(Context context, Variables variables) {
        return new DeletePolicyParams(
                baseParams(context, variables),
                assertPolicyOrArn(variables),
                detachFromResources(variables)
        );
    }

    public static ListPoliciesParams listPolicies(Context context, Variables variables) {
        return new ListPoliciesParams(
                baseParams(context, variables)
        );
    }

    public static AttachPolicyParams attachPolicy(Context context, Variables variables) {
        return new AttachPolicyParams(
                baseParams(context, variables),
                assertRoleName(variables),
                assertPolicyName(variables),
                assertPolicyArn(variables)
        );
    }

    // TODO: for backward compatibility
    private static String assertPolicyOrArn(Variables variables) {
        var result = variables.getString("policy");
        if (result != null) {
            return result;
        }

        return assertPolicyArn(variables);
    }

    private static String assertPolicyArn(Variables variables) {
        return variables.assertString(POLICY_ARN_KEY);
    }

    private static String assertPolicyDocument(Path workDir, Variables variables) {
        return assertFile(workDir, variables, POLICY_FILE_KEY);
    }

    private static String assertPolicyName(Variables variables) {
        return variables.assertString(POLICY_NAME_KEY);
    }

    private static List<Tag> tags(Variables variables) {
        return variables.getMap(TAGS_KEY, Map.of()).entrySet().stream()
                .map(t -> Tag.builder()
                        .key(String.valueOf(t.getKey()))
                        .value(String.valueOf(t.getValue()))
                        .build())
                .toList();
    }

    private static String assertRoleName(Variables variables) {
        return variables.assertString(ROLE_NAME);
    }

    private static String assertTrustPolicy(Path workDir, Variables variables) {
        var policy = variables.getString(TRUST_POLICY_KEY);
        var policyFile = variables.getString(TRUST_POLICY_FILE_KEY);
        if (policy == null && policyFile == null) {
            throw new IllegalArgumentException("Missing required parameter: " + TRUST_POLICY_KEY + " or " + TRUST_POLICY_FILE_KEY);
        }
        if (policyFile != null) {
            return assertFile(workDir, variables, TRUST_POLICY_FILE_KEY);
        }
        return policy;
    }

    private static BaseParams baseParams(Context context, Variables variables) {
        //{ todo: backward compat
        var profile = AwsTaskUtils.getProfile(context);
        var region = AwsTaskUtils.assertRegion(context, variables);
        if (region != null) {
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

    private static boolean detachFromResources(Variables variables) {
        return variables.getBoolean(DETACH_FROM_RESOURCES_KEY, false);
    }
}
