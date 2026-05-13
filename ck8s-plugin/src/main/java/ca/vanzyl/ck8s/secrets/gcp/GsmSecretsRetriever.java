package ca.vanzyl.ck8s.secrets.gcp;

import ca.vanzyl.ck8s.secrets.Secret;
import ca.vanzyl.ck8s.secrets.SecretsManager;
import ca.vanzyl.ck8s.secrets.SecretsRetriever;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

public class GsmSecretsRetriever
        implements SecretsRetriever
{

    private final String secretsDocument;

    public GsmSecretsRetriever(String secretsDocument)
    {
        this.secretsDocument = secretsDocument;
    }

    @Override
    public void delete(String secretName)
    {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void put(String secretName, String value, String description)
    {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public String get(String secretName)
    {
        return map().get(secretName);
    }

    public Map<String, String> map()
    {
        Map<String, String> secrets = Maps.newHashMap();
        GsmClient gsmClient = new GsmClient();
        try {
            String organizationSecretsYaml = gsmClient.get(secretsDocument);
            if (!Strings.isNullOrEmpty(organizationSecretsYaml)) {
                SecretsManager secretsManager = new SecretsManager();
                List<Secret> secretsFromGsm = secretsManager.load(organizationSecretsYaml);
                if (secretsFromGsm != null && !secretsFromGsm.isEmpty()) {
                    for (Secret secret : secretsFromGsm) {
                        secrets.put(secret.name(), secret.value());
                    }
                }
            }
            return secrets;
        }
        finally {
            gsmClient.close();
        }
    }
}
