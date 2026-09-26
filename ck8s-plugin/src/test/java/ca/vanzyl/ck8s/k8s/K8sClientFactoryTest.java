package ca.vanzyl.ck8s.k8s;

import io.fabric8.kubernetes.client.Config;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class K8sClientFactoryTest {

    private static final String EC_SEC1_KEY = """
            -----BEGIN EC PRIVATE KEY-----
            MHcCAQEEIGxaOC6ZI7LALiipvO693jxveRRTiKOCeO0kJMVP5TI6oAoGCCqGSM49
            AwEHoUQDQgAEwQAlC1rV+KW5NkIlj5gB75IJa/tw5+1sx7GR1Y58551PBuKFT92s
            17d4nvepP7ZRspxnag0jLFelDwUvHdErqQ==
            -----END EC PRIVATE KEY-----
            """;

    private static final String PKCS8_KEY = """
            -----BEGIN PRIVATE KEY-----
            MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgwbfEGQaxJGpMtvmV
            n76ZYS/nMn23Ck0gktVjm4W7F1ehRANCAARS2m4+iU4N3t82QtIooq6SuLQCT6Us
            y0OfGTgzyLT0hz8RbdwnfaltzWe/8nihCZKL1iVfDjh45QOvjwZNizXg
            -----END PRIVATE KEY-----
            """;

    @Test
    public void detectsEcClientKey() {
        var config = Config.empty();
        config.setClientKeyData(EC_SEC1_KEY);

        assertEquals("EC", K8sClientFactory.detectClientKeyAlgo(config).getClientKeyAlgo());
    }

    @Test
    public void keepsTheDefaultForAnUndetectableClientKey() {
        var config = Config.empty();
        config.setClientKeyData(PKCS8_KEY);
        var expected = config.getClientKeyAlgo();

        assertEquals(expected, K8sClientFactory.detectClientKeyAlgo(config).getClientKeyAlgo());
    }

    @Test
    public void keepsTheDefaultWithoutAClientKey() {
        var config = Config.empty();
        var expected = config.getClientKeyAlgo();

        assertEquals(expected, K8sClientFactory.detectClientKeyAlgo(config).getClientKeyAlgo());
    }
}
