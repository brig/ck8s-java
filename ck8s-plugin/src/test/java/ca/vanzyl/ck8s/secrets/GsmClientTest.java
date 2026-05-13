package ca.vanzyl.ck8s.secrets;

import ca.vanzyl.ck8s.secrets.gcp.GsmClient;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class GsmClientTest
{
    @Test
    @Ignore
    public void validateAsmClientReturnsNullOnMissingSecret()
            throws IOException
    {
        GsmClient client = new GsmClient();
        String kubeconfigName = "this-secret-does-not-exist";
        String kubeconfigContent = client.get(kubeconfigName);
        assertThat(kubeconfigContent).isNull();
    }
}
