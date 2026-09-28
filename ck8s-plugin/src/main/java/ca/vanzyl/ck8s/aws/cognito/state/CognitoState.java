package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import ca.vanzyl.ck8s.state.EntityState;
import com.walmartlabs.concord.runtime.v2.sdk.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CognitoState {

    private final CognitoClientFactory clientFactory;
    private final EntityState state;

    @Inject
    public CognitoState(EntityState state, CognitoClientFactory clientFactory) {
        this.state = state;
        this.clientFactory = clientFactory;
    }

    public UserPool userPoolByName(Context context, CognitoTaskParams.BaseParams baseParams, String poolName, String poolId) {
        var userPoolId = state.getOrLoad(new UserPoolIdKey(poolName),
                new CognitoUserPoolIdLoader(context, clientFactory, baseParams.profile(), baseParams.region()));

        if (userPoolId == null) {
            state.put(new UserPoolKey(poolId), null);
            return null;
        }

        return userPoolById(context, baseParams, userPoolId.id());
    }

    public UserPool userPoolById(Context context, CognitoTaskParams.BaseParams baseParams, String poolId) {
        return state.getOrLoad(new UserPoolKey(poolId),
                new CognitoUserPoolByIdLoader(context, clientFactory, baseParams.profile(), baseParams.region()));
    }

    public void put(UserPool pool) {
        state.put(new UserPoolKey(pool.id()), pool);
    }

    public IdentityProvider identityProvider(Context context, CognitoTaskParams.BaseParams baseParams,
                                             String poolId,
                                             String providerName) {
        return state.getOrLoad(new IdentityProviderKey(poolId, providerName),
                new CognitoIdentityProviderLoader(context, clientFactory, baseParams.profile(), baseParams.region()));
    }

    public void put(IdentityProvider identityProvider) {
        state.put(new IdentityProviderKey(identityProvider.poolId(), identityProvider.name()), identityProvider);
    }

    public ResourceServer resourceServer(Context context, CognitoTaskParams.BaseParams baseParams, String poolId, String identifier) {
        return state.getOrLoad(new ResourceServerKey(poolId, identifier),
                new ResourceServerLoader(context, clientFactory, baseParams.profile(), baseParams.region()));
    }

    public void put(ResourceServer resourceServer) {
        state.put(new ResourceServerKey(resourceServer.poolId(), resourceServer.identifier()), resourceServer);
    }

    public UserPoolClient userPoolClient(Context context, CognitoTaskParams.BaseParams baseParams, String poolId, String clientName) {
        return state.getOrLoad(new UserPoolClientKey(poolId, clientName),
                new UserPoolClientLoader(context, clientFactory, baseParams.profile(), baseParams.region()));
    }

    public void put(UserPoolClient userPoolClient) {
        state.put(new UserPoolClientKey(userPoolClient.poolId(), userPoolClient.clientName()), userPoolClient);
    }

    public UserPoolUser user(Context context, CognitoTaskParams.BaseParams baseParams, String poolId, String username) {
        return state.getOrLoad(new UserPoolUserKey(poolId, username),
                new UserPoolUserLoader(context, clientFactory, baseParams.profile(), baseParams.region()));
    }

    public void put(UserPoolUser user) {
        state.put(new UserPoolUserKey(user.poolId(), user.username()), user);
    }
}
