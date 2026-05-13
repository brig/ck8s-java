package ca.vanzyl.concord.k8s;

import com.walmartlabs.concord.common.validation.ConcordKey;
import com.walmartlabs.concord.server.user.UserType;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public record ApiKeyRequest(UUID userId, String username, String userDomain, UserType userType,
                            @ConcordKey String name, String key) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
