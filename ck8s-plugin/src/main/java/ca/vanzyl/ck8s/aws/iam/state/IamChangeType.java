package ca.vanzyl.ck8s.aws.iam.state;

public final class IamChangeType {

    public static final String ROLE_TYPE = "aws:iam:role";
    public static final String ROLE_TAG_TYPE = "aws:iam:role:tag";
    public static final String TRUST_POLICY_TYPE = "aws:iam:role:trust-policy";
    public static final String INLINE_POLICY_TYPE = "aws:iam:inline-policy";
    public static final String MANAGED_POLICY_TYPE = "aws:iam:policy";
    public static final String MANAGED_POLICY_ATTACH_TYPE = "aws:iam:policy-attach";

    public static String roleId(String roleName) {
        return String.format("%s:%s", ROLE_TYPE, roleName);
    }

    public static String roleTagId(String roleName, String tagKey) {
        return roleId(roleName) + ":tag:" + tagKey;
    }

    public static String trustPolicyId(String roleName) {
        return roleId(roleName) + ":trust-policy";
    }

    public static String inlinePolicyId(String roleName, String policyName) {
        return roleId(roleName) + ":inline-policy:" + policyName;
    }

    public static String managedPolicyId(String policyArn) {
        return String.format("%s:%s", MANAGED_POLICY_TYPE, policyArn);
    }

    public static String managedPolicyAttachId(String roleName, String policyName) {
        return roleId(roleName) + ":policy-attach:" + policyName;
    }

    private IamChangeType() {
    }
}
