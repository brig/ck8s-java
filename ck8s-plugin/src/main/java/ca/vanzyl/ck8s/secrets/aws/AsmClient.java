package ca.vanzyl.ck8s.secrets.aws;

import ca.vanzyl.ck8s.aws.CredentialsProvider;
import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.*;

import java.util.function.Function;

public class AsmClient
{

    private final static Logger log = LoggerFactory.getLogger(AsmClient.class);

    private final SecretsManagerClient client;

    public AsmClient(CredentialsProvider credentialsProvider, String region, String profile)
    {
        client = SecretsManagerClient.builder()
                .credentialsProvider(credentialsProvider.get(profile))
                .region(Region.of(region))
                .build();
    }

    public void update(String secretName, Function<String, String> updater)
    {
        GetSecretValueResponse secretBeforeUpdate = get(secretName);
        if (secretBeforeUpdate == null) {
            throw new UserDefinedException("Secret '" + secretName + "' not found");
        }

        UpdateSecretResponse updateResponse = client.updateSecret(UpdateSecretRequest.builder()
                .secretId(secretName)
                .secretString(updater.apply(secretBeforeUpdate.secretString()))
                .build());

        log.info("update ['{}'] -> ok, resp: {}", secretName, updateResponse);
    }

    public GetSecretValueResponse get(String secretName)
    {
        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

        try {
            return client.getSecretValue(request);
        }
        catch (ResourceNotFoundException e) {
            return null;
        }
    }
}
