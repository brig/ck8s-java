package ca.vanzyl.ck8s.aws.iam.state;

import ca.vanzyl.ck8s.state.EntityKey;

public record IamRoleKey (String roleName) implements EntityKey<IamRole> {

    @Override
    public Class<IamRole> entityType() {
        return IamRole.class;
    }
}
