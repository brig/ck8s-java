package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.state.EntityKey;

public record IdentityProviderKey(String poolId, String providerName) implements EntityKey<IdentityProvider> {

    @Override
    public Class<IdentityProvider> entityType() {
        return IdentityProvider.class;
    }
}
