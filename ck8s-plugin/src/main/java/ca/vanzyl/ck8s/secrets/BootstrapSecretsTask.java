package ca.vanzyl.ck8s.secrets;

import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

@Named("secrets")
@DryRunReady
public class BootstrapSecretsTask
        extends FakeMap
        implements Task
{

    private final static Logger log = LoggerFactory.getLogger(BootstrapSecretsTask.class);

    private final Context context;
    private final SecretsRetrieverProvider secretsRetrieverProvider;

    private static final long SECRET_POLL_INTERVAL = TimeUnit.SECONDS.toMillis(5);

    private final boolean dryRunMode;

    @Inject
    public BootstrapSecretsTask(Context context, SecretsRetrieverProvider secretsRetrieverFactory)
    {
        this.context = context;
        this.dryRunMode = context.processConfiguration().dryRun();
        this.secretsRetrieverProvider = secretsRetrieverFactory;
    }

    @Override
    @SensitiveData
    public String getOrDefault(Object key, String defaultValue) {
        return super.getOrDefault(key, defaultValue);
    }

    @Override
    @SensitiveData
    public String get(Object key)
    {
        return get(null, key);
    }

    @SensitiveData
    public String get(String secretDocument, Object key)
    {
        return secret(secretDocument, (String) key);
    }

    @SensitiveData
    public String waitFor(String key, long timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        while (!Thread.currentThread().isInterrupted()) {
            String result = get(key);
            if (result != null) {
                return result;
            }

            if (System.currentTimeMillis() - startTime > (timeoutSeconds * 1000)) {
                throw new UserDefinedException("Timeout waiting for secret '" + key + "'");
            }

            log.info("waitForSecret ['{}'] -> no secret, waiting {}ms before retry", key, SECRET_POLL_INTERVAL);

            sleep(SECRET_POLL_INTERVAL);
        }

        throw new UserDefinedException("Secret with '" + key + "' not found");
    }

    public boolean has(String key) {
        return has(null, key);
    }

    public boolean has(String secretsDocument, String key) {
        String result = get(secretsDocument, key);
        return result != null && !result.trim().isEmpty();
    }

    public void delete(String secretName)
    {
        delete(null, secretName);
    }

    public void delete(String secretsDocument, String secretName)
    {
        SecretsRetriever retriever = secretsRetrieverProvider.create(context, secretsDocument);

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping deleting secret value with key '{}'", secretName);
            return;
        }

        log.info("Deleting secret '{}' ({})", secretName, retriever);
        retriever.delete(secretName);
    }

    @SensitiveData
    public String computeIfAbsent(String secretName, String value, String description)
    {
        return computeIfAbsent(null, secretName, value, description);
    }

    @SensitiveData
    public String computeIfAbsent(String secretsDocument, String secretName, String value, String description)
    {
        String existing = get(secretsDocument, secretName);
        if (existing != null) {
            log.info("Secret '{}' already exists, skipping", secretName);
            return existing;
        }

        put(secretsDocument, secretName, value, description);
        return value;
    }

    public void put(String secretName, String value, String description)
    {
        put(null, secretName, value, description);
    }

    public void put(String secretsDocument, String secretName, String value, String description)
    {
        if (value == null || value.isBlank()) {
            throw new UserDefinedException("Failed to put the secret '" + secretName + "' into '" + secretsDocument + "' document: empty value");
        }

        SecretsRetriever retriever = secretsRetrieverProvider.create(context, secretsDocument);

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping adding secret value with key '{}'", secretName);
            return;
        }

        log.info("Adding secret '{}' ({})", secretName, retriever);
        retriever.put(secretName, value, description);
    }

    @SensitiveData
    public String secret(String secretsDocument, String secretName)
    {
        SecretsRetriever retriever = secretsRetrieverProvider.create(context, secretsDocument);

        String value = retriever.get(secretName);
        if (value == null || value.trim().isEmpty()) {
            log.warn("got empty value for the secret '{}'", secretName);
            return null;
        }
        return value;
    }

    private static void sleep(long ms)
    {
        try {
            Thread.sleep(ms);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
