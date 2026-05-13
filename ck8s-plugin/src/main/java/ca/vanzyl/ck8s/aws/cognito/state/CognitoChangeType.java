package ca.vanzyl.ck8s.aws.cognito.state;

public final class CognitoChangeType {

    public static final String USER_POOL_TYPE = "aws:cognito:user-pool";
    public static final String IDENTITY_PROVIDER_TYPE = "aws:cognito:identity-provider";
    public static final String RESOURCE_SERVER_TYPE = "aws:cognito:resource-server";
    public static final String USER_POOL_CLIENT_TYPE = "aws:cognito:user-pool-client";
    public static final String USER_POOL_USER_TYPE = "aws:cognito:user-pool-user";

    public static String userPoolId(String poolId) {
        return String.format("%s:%s", USER_POOL_TYPE, poolId);
    }

    public static String identityProviderId(String poolId, String providerName) {
        return userPoolId(poolId) + ":identity-provider:" + providerName;
    }

    public static String resourceServerId(String poolId, String identifier) {
        return userPoolId(poolId) + ":resource-server:" + identifier;
    }

    public static String userPoolClientId(String poolId, String clientName) {
        return userPoolId(poolId) + ":user-pool-client:" + clientName;
    }

    public static String userPoolUserId(String poolId, String username) {
        return userPoolId(poolId) + ":user-pool-user:" + username;
    }

//
//    public static String roleTagId(String roleName, String tagKey) {
//        return roleId(roleName) + ":tag:" + tagKey;
//    }
//
//    public static String trustPolicyId(String roleName) {
//        return roleId(roleName) + ":trust-policy";
//    }
//
//    public static String inlinePolicyId(String roleName, String policyName) {
//        return roleId(roleName) + ":inline-policy:" + policyName;
//    }
//
//    public static String managedPolicyId(String policyArn) {
//        return String.format("%s:%s", MANAGED_POLICY_TYPE, policyArn);
//    }
//
//    public static String managedPolicyAttachId(String roleName, String policyName) {
//        return roleId(roleName) + ":policy-attach:" + policyName;
//    }

    private CognitoChangeType() {
    }
}
