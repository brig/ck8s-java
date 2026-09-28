package ca.vanzyl.ck8s.secrets.aws;

import ca.vanzyl.ck8s.aws.CredentialsProvider;
import ca.vanzyl.ck8s.secrets.*;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class AsmSecretsRetriever
        implements SecretsRetriever {

    private final static Logger log = LoggerFactory.getLogger(AsmSecretsRetriever.class);

    private final DocumentKey documentKey;
    private final boolean debug;
    private final boolean useCache;
    private final CredentialsProvider credentialsProvider;

    private final Cache<DocumentKey, Map<String, String>> cache = CacheBuilder.newBuilder().build();

    public AsmSecretsRetriever(String region, String profile, String secretsDocument,
                               boolean debug, boolean cacheSecretsDocument,
                               CredentialsProvider credentialsProvider) {
        this.documentKey = new DocumentKey(region, profile, secretsDocument);
        this.debug = debug;
        this.useCache = cacheSecretsDocument;
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public String get(Context context, String key) {
        String result = map(context).get(key);

        if (result == null || result.trim().isEmpty()) {
            log.info("get ['{}', '{}'] -> secret value not found ('{}')",
                    documentKey, key, result);
        }

        return result;
    }

    @Override
    public Map<String, String> map(Context context) {
        if (useCache) {
            try {
                return cache.get(documentKey, () -> fetchSecretsDocumentAsMap(context, documentKey));
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return fetchSecretsDocumentAsMap(context, documentKey);
    }

    private Map<String, String> fetchSecretsDocumentAsMap(Context context, DocumentKey key) {
        GetSecretValueResponse response = new AsmClient(credentialsProvider, context, key.region(), key.profile()).get(key.secretsDocument());
        if (response == null) {
            log.warn("fetchSecretsDocumentAsMap ['{}'] -> null response from asm", key.secretsDocument());
            return null;
        }

        if (Strings.isNullOrEmpty(response.secretString())) {
            log.warn("fetchSecretsDocumentAsMap ['{}'] -> empty secretString, response is {}", key.secretsDocument(), response);
            return null;
        }

        try {
            List<Secret> secretsFromAsm = new SecretsManager().load(response.secretString());
            if (secretsFromAsm == null) {
                return null;
            }

            return secretsFromAsm.stream()
                    .collect(Collectors.toMap(Secret::name, Secret::value));
        } catch (SecretsManagerException e) {
            throw new RuntimeException("Failed to load the ASM document '" + key + "': " + e.getMessage());
        }
    }

    @Override
    public void delete(Context context, String key) {
        new AsmClient(credentialsProvider, context, documentKey.region, documentKey.profile)
                .update(documentKey.secretsDocument, secret -> {
                    if (debug) {
                        log.info("Removing '{}' from secret '{}'", key, documentKey);
                    }

                    if (Strings.isNullOrEmpty(secret)) {
                        log.info("Secret document '{}' is empty -> nothing to delete", documentKey);
                        return secret;
                    }

                    cache.invalidate(documentKey);

                    List<Secret> secrets = new SecretsManager().load(secret).stream()
                            .filter(s -> !s.name().equals(key))
                            .collect(Collectors.toList());

                    return new SecretsManager().serialize(ImmutableSecrets.builder().list(secrets).build());
                });
    }

    @Override
    public void put(Context context, String key, String value, String description) {
        new AsmClient(credentialsProvider, context, documentKey.region, documentKey.profile)
                .update(documentKey.secretsDocument, secret -> {
                    if (debug) {
                        log.info("Updating secret '{}' with '{}'", documentKey, key);
                    }

                    cache.invalidate(documentKey);

                    List<Secret> secrets = new ArrayList<>();
                    if (!Strings.isNullOrEmpty(secret)) {
                        List<Secret> currentSecrets = new SecretsManager().load(secret);
                        for (Secret s : currentSecrets) {
                            if (!key.equals(s.name())) {
                                secrets.add(s);
                            }
                        }
                    }

                    secrets.add(ImmutableSecret.builder()
                            .name(key)
                            .value(value)
                            .description(description)
                            .build());

                    return new SecretsManager().serialize(ImmutableSecrets.builder().list(secrets).build());
                });
    }

    @Override
    public String toString() {
        return "aws region: '" + documentKey.region + "', account '" + documentKey.profile + "', secretsDocument: '" + documentKey.secretsDocument + "'";
    }

    private record DocumentKey(String region, String profile, String secretsDocument) {
    }
}
