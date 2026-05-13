package ca.vanzyl.ck8s.secrets;

import ca.vanzyl.ck8s.aws.CredentialsProvider;
import ca.vanzyl.ck8s.common.MapUtils;
import ca.vanzyl.ck8s.secrets.aws.AsmSecretsRetriever;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.walmartlabs.concord.runtime.v2.sdk.Context;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Singleton
public class SecretsRetrieverProvider {

    private final CredentialsProvider credentialsProvider;

    private final LoadingCache<Key, SecretsRetriever> cache = CacheBuilder.newBuilder()
            .build(new CacheLoader<>() {
                @Override
                public SecretsRetriever load(Key key) {
                    return create(key);
                }
            });

    @Inject
    public SecretsRetrieverProvider(CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    public SecretsRetriever create(Context context, String secretsDocument) {
        Map<String, Object> clusterRequest = context.variables().getMap("clusterRequest", context.defaultVariables().getMap("localCluster", null));
        if (clusterRequest == null) {
            throw new RuntimeException("The clusterRequest is null. Cannot proceed.");
        }

        String provider = getProvider(clusterRequest);
        String region = MapUtils.assertString(clusterRequest, "aws.homeRegion");
        String account = MapUtils.getString(clusterRequest, "account");
        String document = secretsDocument != null ? secretsDocument : MapUtils.assertString(clusterRequest, "aws.secretsDocument");

        try {
            return cache.get(new Key(provider, region, account, document));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private SecretsRetriever create(Key key) {
        if ("aws".equals(key.provider)) {
            return new AsmSecretsRetriever(key.region, key.profile, key.secretsDocument, true, true, credentialsProvider);
        } else if ("local".equals(key.provider)) {
            return new LocalSecretsRetriever(key.secretsDocument, true);
        } else if ("fake".equals(key.provider)) {
            return new FakeSecretsRetriever();
        }
        throw new RuntimeException("Unknown provider '" + key.provider + "'");
    }

    private static String getProvider(Map<String, Object> clusterRequest) {
        String secretsProvider = MapUtils.getString(clusterRequest, "secretsProvider");
        if (secretsProvider != null) {
            return secretsProvider;
        }
        return MapUtils.getString(clusterRequest, "provider");
    }

    private record Key(String provider, String region, String profile, String secretsDocument) {
    }
}
