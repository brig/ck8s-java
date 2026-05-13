package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.state.EntityKey;

public record UserPoolIdKey(String poolName) implements EntityKey<UserPoolId> {

    @Override
    public Class<UserPoolId> entityType() {
        return UserPoolId.class;
    }
}
