package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.state.Entity;
import ca.vanzyl.ck8s.state.EntityKey;
import ca.vanzyl.ck8s.state.EntityLoader;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

public abstract class AbstractCognitoEntityLoader<K extends EntityKey<E>, E extends Entity> implements EntityLoader<K, E> {

    private final CognitoClientFactory clientFactory;
    private final String profile;
    private final Region region;

    public AbstractCognitoEntityLoader(CognitoClientFactory clientFactory, String profile, Region region) {
        this.clientFactory = clientFactory;
        this.region = region;
        this.profile = profile;
    }

    protected CognitoIdentityProviderClient createClient() {
        return clientFactory.create(profile, region);
    }
}
