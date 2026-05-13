package ca.vanzyl.ck8s.secrets.gcp;

import ca.vanzyl.ck8s.secrets.SecretsProvider;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersion;

import java.io.IOException;

public class GsmClient
        implements SecretsProvider
{

    private SecretManagerServiceClient secretManagerServiceClient;

    public GsmClient()
    {
        //region is not used in GCP keeping the same API Across
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
            credentials.refreshIfExpired();
            AccessToken token = credentials.getAccessToken();
            secretManagerServiceClient = SecretManagerServiceClient.create();
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    //Secrets are in the format `projects/*/secrets/*`.
    @Override
    public String get(String secretName)
    {
        String secretPayload = "";
        if (secretManagerServiceClient != null) {
            if (!secretName.contains("/versions/")) {
                secretName = String.format("%s/%s/%s", secretName, "versions", "latest");
            }
            SecretVersion response = secretManagerServiceClient.getSecretVersion(secretName);
            AccessSecretVersionResponse sresponse = secretManagerServiceClient.accessSecretVersion(response.getName());
            secretPayload = sresponse.getPayload().getData().toStringUtf8();
            return secretPayload;
        }
        return null;
    }

    public void close()
    {
        if (secretManagerServiceClient != null) {
            secretManagerServiceClient.close();
            secretManagerServiceClient = null;
        }
    }
}
