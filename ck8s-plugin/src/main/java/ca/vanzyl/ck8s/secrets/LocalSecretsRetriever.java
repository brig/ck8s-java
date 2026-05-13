package ca.vanzyl.ck8s.secrets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class LocalSecretsRetriever
        implements SecretsRetriever
{

    private final static Logger log = LoggerFactory.getLogger(LocalSecretsRetriever.class);

    private final String secretsDocument;
    private final boolean debug;

    public LocalSecretsRetriever(String secretsDocument, boolean debug)
    {
        this.secretsDocument = secretsDocument;
        this.debug = debug;
    }

    @Override
    public String get(String key)
    {
        if (debug) {
            log.info("loading secret '{}' from '{}' document", key, secretsDocument);
        }

        Path documentPath = documentPath(secretsDocument);
        if (Files.notExists(documentPath)) {
            throw new RuntimeException("secrets document not found: " + documentPath);
        }

        List<Secret> secrets;
        try {
            secrets = new SecretsManager().load(new String(Files.readAllBytes(documentPath)));
        } catch (SecretsManagerException e) {
            throw new RuntimeException("Failed to get the local secret '" + key + "': " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return secrets.stream()
                .filter(s -> s.name().equals(key))
                .findAny()
                .map(Secret::value).orElse(null);
    }

    @Override
    public Map<String, String> map() {
        return null;
    }

    @Override
    public void delete(String key)
    {
        log.warn("Delete operation ignored");
    }

    private static Path documentPath(String secretsDocument) {
        Path result = Path.of(System.getProperty("user.home"))
                .resolve(".ck8s")
                .resolve("secrets")
                .resolve(secretsDocument + ".yaml");

        if (Files.exists(result)) {
            return result;
        }

        return Path.of(System.getProperty("user.dir"))
                .resolve("secrets")
                .resolve(secretsDocument + ".yaml");
    }

    @Override
    public void put(String key, String value, String description)
    {
        log.warn("Update operation ignored");
    }

    @Override
    public String toString()
    {
        return "local secretsDocument: '" + secretsDocument + "'";
    }
}
