package ca.vanzyl.ck8s.aws.iam.state;

import ca.vanzyl.ck8s.state.EntityKey;

public record IamInlinePolicyKey(String roleName, String policyName) implements EntityKey<IamInlinePolicy> {
    @Override
    public Class<IamInlinePolicy> entityType() {
        return IamInlinePolicy.class;
    }
}
