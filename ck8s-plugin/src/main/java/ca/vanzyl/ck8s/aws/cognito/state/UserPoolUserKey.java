package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.state.EntityKey;

public record UserPoolUserKey(String poolId, String username) implements EntityKey<UserPoolUser> {

    @Override
    public Class<UserPoolUser> entityType() {
        return UserPoolUser.class;
    }
}
