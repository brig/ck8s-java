package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.state.EntityKey;

public record UserPoolKey(String poolId) implements EntityKey<UserPool> {

    @Override
    public Class<UserPool> entityType() {
        return UserPool.class;
    }
}
