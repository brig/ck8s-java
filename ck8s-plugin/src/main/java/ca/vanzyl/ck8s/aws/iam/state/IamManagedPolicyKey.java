package ca.vanzyl.ck8s.aws.iam.state;

import ca.vanzyl.ck8s.state.EntityKey;

public record IamManagedPolicyKey (String policyArn) implements EntityKey<IamManagedPolicy> {
    @Override
    public Class<IamManagedPolicy> entityType() {
        return IamManagedPolicy.class;
    }
}
