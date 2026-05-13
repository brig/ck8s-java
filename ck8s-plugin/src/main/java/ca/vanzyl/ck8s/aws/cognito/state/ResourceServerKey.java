package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.state.EntityKey;

public record ResourceServerKey(String poolId, String identifier) implements EntityKey<ResourceServer> {

    @Override
    public Class<ResourceServer> entityType() {
        return ResourceServer.class;
    }
}
