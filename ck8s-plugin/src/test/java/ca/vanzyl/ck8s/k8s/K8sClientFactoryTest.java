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

    private static final String EC_PKCS8_KEY = """
            -----BEGIN PRIVATE KEY-----
            MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgwbfEGQaxJGpMtvmV
            n76ZYS/nMn23Ck0gktVjm4W7F1ehRANCAARS2m4+iU4N3t82QtIooq6SuLQCT6Us
            y0OfGTgzyLT0hz8RbdwnfaltzWe/8nihCZKL1iVfDjh45QOvjwZNizXg
            -----END PRIVATE KEY-----
            """;

    private static final String RSA_PKCS8_KEY = """
            -----BEGIN PRIVATE KEY-----
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCNGLWPRr7eKM3K
            tT0/6N0H3YVBP7kyu8hLND6C2xhg5vNY4AoL/63fR0aepWMJB5EoSv2rrO2Yc4pA
            AqwMGOAd2pj3+sUtRjBRiOXxy/cLFJgM85jNmamVOGpoHy38EwgXwysTPaztjCUh
            /7RCWq82+pJaj1Hu4tUXb7NhcZv7ory9tXp7f3nEO47qJO9xoJ8vZlR3z1tFQOWY
            h/+AJ3pYrG3Pvkaxfd7Kq3VkhDna3OOu57zteL2Uom+JbfbQ0WYNp+ItjqgMkHh3
            tBs38IGOS23McFWJ23WY+1djI4GGwsq5T2wP5ubWDilzRXycVODDqEQR+2TQh1VM
            twZPEKKVAgMBAAECggEACAF/Kcmcl7aZKKznHwrLjIBIqOwLSizmqkJlCp/mY/cu
            wcaijfg8Xsx982SpzXpxBX2DI7wnQMIg4HgWWDBgJRQryk71DwLUx9znQ99BcvmV
            6rmMBxx2h27roLamMWTzYwUcsFXh9kTrBXZraA7FCC79t2bll1L7vtLvIB1mDvLL
            xSUz9sM3uDrECzSj82XULd8pbZUt3tnIaxqi21u4OHWShq03hX2ZjjXBlmpQrcYR
            0Bt2Trt1FSTon5zERGHfuEcRKkvBeCCzveXXdfy1ZUlvWThC8htAjpkcyVkRcPQF
            BjW9IUyKXDudxLojXoYjAZDAfYMal7WNru3etrJ2UQKBgQDCfVqa6yNJnpTzdICh
            4FAsU8Q8gR2rRT67j2AbtG3J4EJMvtOq8VJ9OXK6PbPaDGJQ2d6z3rt3ZqUCBGf3
            2DZjClarUjJWdsIAsNcf5F0tQxgJc5xkPrcPYt0PR1mdDI3kGkroR7Yu+H8N5Qqj
            GJ1BRx9L9kT6ToH+9PW+f2z9RQKBgQC5uG/c7to3+dU9yGbgz0KDmJW7l+aaRard
            GvQt/Ieypko+g5BtVf46Zxi+iTzyADxJiUKJd0KFmDuwkliw4v2N2SXqa+K3m5Dr
            W6gjDh+vpje5vJGWy+T6PC0jCIop6ny6oyogMoe5sC5sNfBavVskKZniNB9fQlYc
            ByK8WkcdEQKBgQCLKsWdmunKMRZmSpQMwQS9Y0dFACLpvgfQkBx2VppE3Rqz7cxq
            QnoDLwtgJuy99ySWs/9+d79vBdzG375Bg84O+oPf3VY1to7FjcSxhmgCDkNx5+8d
            cB7vfI+v8h82mJgjg4jcQwwi+h94Is+EuwUzg5/qbBMZhPnSJRh/MpEbfQKBgAx+
            jQAHDaVAaIksh0dOikICLOie8oOkdjdDzfOeDp8FMu97uGayp1TUhMSkxPXe8C9e
            TZyj6lTEhhd98PuNedNmLXfU5D7H93ruAqTBGX6epxcWyZCkjPYMwFigBXOGc/e2
            bGoYHCtw396lzlK8dHo9Vj3ylb2538Mo87xKMoiRAoGAVZypHk0ECEvWpzEyHYkd
            69D4QAXuSgb4kabtkz8JTiwM8H9HXudLOpTliPU6kRk2b4nHONbHFvf/vnxyijsl
            zb2nEwyPhgLhUpmGaNX7iW6r+Le/SrnVX7V0s/jwyVf+hheyyvEdQ2LFutZVQn40
            gJs+1ciN5tLSIOUYBiQwEm4=
            -----END PRIVATE KEY-----
            """;

    private static final String ED25519_PKCS8_KEY = """
            -----BEGIN PRIVATE KEY-----
            MC4CAQAwBQYDK2VwBCIEIFbBuR+WPbkU0FYZAI6OBABlySG56yATC3NQ2NlZIfHH
            -----END PRIVATE KEY-----
            """;

    @Test
    public void detectsEcClientKey() {
        var config = Config.empty();
        config.setClientKeyData(EC_SEC1_KEY);

        assertEquals("EC", K8sClientFactory.detectClientKeyAlgo(config).getClientKeyAlgo());
    }

    // what pinniped actually hands back: the header names no algorithm, so it has to be read from the key
    @Test
    public void detectsEcClientKeyInPkcs8() {
        var config = Config.empty();
        config.setClientKeyData(EC_PKCS8_KEY);

        assertEquals("EC", K8sClientFactory.detectClientKeyAlgo(config).getClientKeyAlgo());
    }

    @Test
    public void detectsRsaClientKeyInPkcs8() {
        var config = Config.empty();
        config.setClientKeyData(RSA_PKCS8_KEY);

        assertEquals("RSA", K8sClientFactory.detectClientKeyAlgo(config).getClientKeyAlgo());
    }

    @Test
    public void keepsTheDefaultForAnUndetectableClientKey() {
        var config = Config.empty();
        config.setClientKeyData(ED25519_PKCS8_KEY);
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
