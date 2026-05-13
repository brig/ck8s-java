package ca.vanzyl.ck8s.aws;

import software.amazon.awssdk.services.sts.model.Credentials;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

public record SessionCredentials(String accessKeyId, String secretAccessKey, String sessionToken,
                                 Instant expiration) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static SessionCredentials from(Credentials credentials) {
        return new SessionCredentials(credentials.accessKeyId(), credentials.secretAccessKey(), credentials.sessionToken(), credentials.expiration());
    }
}
