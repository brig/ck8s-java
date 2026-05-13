package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.state.EntityKey;

public record UserPoolClientKey(String poolId, String clientName) implements EntityKey<UserPoolClient> {

    @Override
    public Class<UserPoolClient> entityType() {
        return UserPoolClient.class;
    }
}
