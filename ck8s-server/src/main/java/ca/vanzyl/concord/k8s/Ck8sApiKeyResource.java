package ca.vanzyl.concord.k8s;

import com.walmartlabs.concord.common.validation.ConcordKey;
import com.walmartlabs.concord.db.AbstractDao;
import com.walmartlabs.concord.db.MainDB;
import com.walmartlabs.concord.db.PgUtils;
import com.walmartlabs.concord.server.OperationResult;
import com.walmartlabs.concord.server.audit.AuditAction;
import com.walmartlabs.concord.server.audit.AuditLog;
import com.walmartlabs.concord.server.audit.AuditObject;
import com.walmartlabs.concord.server.cfg.ApiKeyConfiguration;
import com.walmartlabs.concord.server.sdk.ConcordApplicationException;
import com.walmartlabs.concord.server.sdk.rest.Resource;
import com.walmartlabs.concord.server.sdk.validation.Validate;
import com.walmartlabs.concord.server.sdk.validation.ValidationErrorsException;
import com.walmartlabs.concord.server.security.Roles;
import com.walmartlabs.concord.server.security.UnauthorizedException;
import com.walmartlabs.concord.server.security.UserPrincipal;
import com.walmartlabs.concord.server.security.apikey.ApiKeyDao;
import com.walmartlabs.concord.server.security.apikey.CreateApiKeyResponse;
import com.walmartlabs.concord.server.user.UserManager;
import com.walmartlabs.concord.server.user.UserType;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.shiro.authz.AuthorizationException;
import org.jooq.Configuration;
import org.jooq.exception.DataAccessException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.OffsetDateTime;
import java.util.UUID;

import static com.walmartlabs.concord.server.jooq.tables.ApiKeys.API_KEYS;
import static com.walmartlabs.concord.server.security.apikey.ApiKeyUtils.hash;

@Named
@Singleton
@Path("/api/ck8s/v1/apikey")
public class Ck8sApiKeyResource implements Resource {

    private final ApiKeyConfiguration cfg;
    private final ApiKeyDao apiKeyDao;
    private final Dao dao;
    private final AuditLog auditLog;
    private final UserManager userManager;

    @Inject
    public Ck8sApiKeyResource(ApiKeyConfiguration cfg, ApiKeyDao apiKeyDao,
                              Dao dao, AuditLog auditLog, UserManager userManager) {
        this.cfg = cfg;
        this.apiKeyDao = apiKeyDao;
        this.dao = dao;
        this.auditLog = auditLog;
        this.userManager = userManager;
    }

    @POST
    @Path("/{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Validate
    @Operation(description = "Create a new API key", operationId = "createApiKey")
    public CreateApiKeyResponse create(@PathParam("name") @ConcordKey String name) {
        assertAdmin();

        return createOrUpdateApiKey(name);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Validate
    @Operation(description = "Create a new API key", operationId = "createUserApiKey")
    public CreateApiKeyResponse create(@Valid ApiKeyRequest req) {
        UUID userId = assertUserId(req.userId());
        if (userId == null) {
            userId = assertUsername(req.username(), req.userDomain(), req.userType());
        }

        if (userId == null) {
            userId = UserPrincipal.assertCurrent().getId();
        }

        assertOwner(userId);

        String name = trim(req.name());
        if (name == null || name.isEmpty()) {
            // auto generate the name
            name = "key#" + UUID.randomUUID();
        }

        if (apiKeyDao.getId(userId, name) != null) {
            throw new ValidationErrorsException("API Token with name '" + name + "' already exists");
        }

        return createApiKey(userId, name, req.key());
    }

    private CreateApiKeyResponse createOrUpdateApiKey(String name) {
        String key = apiKeyDao.newApiKey();

        OffsetDateTime expiredAt = null;
        if (cfg.isExpirationEnabled()) {
            expiredAt = OffsetDateTime.now().plusDays(cfg.getExpirationPeriod().toDays());
        }

        UUID id = dao.createOrUpdateApiKey(name, key, expiredAt);

        auditLog.add(AuditObject.API_KEY, AuditAction.CREATE)
                .field("id", id)
                .field("name", name)
                .field("expiredAt", expiredAt)
                .log();

        return new CreateApiKeyResponse(id, name, key,  OperationResult.CREATED);
    }

    private CreateApiKeyResponse createApiKey(UUID userId, String name, String key) {
        if (key == null || key.isEmpty()) {
            key = apiKeyDao.newApiKey();
        }

        OffsetDateTime expiredAt = null;
        if (cfg.isExpirationEnabled()) {
            expiredAt = OffsetDateTime.now().plusDays(cfg.getExpirationPeriod().toDays());
        }

        UUID id;
        try {
            id = apiKeyDao.insert(userId, key, name, expiredAt);
        } catch (DataAccessException e) {
            if (PgUtils.isUniqueViolationError(e)) {
                throw new ValidationErrorsException("Duplicate API key name: " + name);
            }

            throw e;
        }

        auditLog.add(AuditObject.API_KEY, AuditAction.CREATE)
                .field("id", id)
                .field("name", name)
                .field("expiredAt", expiredAt)
                .field("userId", userId)
                .log();

        return new CreateApiKeyResponse(id, name, key,  OperationResult.CREATED);
    }

    private static void assertAdmin() {
        if (!Roles.isAdmin()) {
            throw new AuthorizationException("Only admins are allowed to update organizations");
        }
    }

    private static void assertOwner(UUID userId) {
        if (Roles.isAdmin()) {
            // admin users can manage other user's keys
            return;
        }

        UserPrincipal p = UserPrincipal.assertCurrent();
        if (!userId.equals(p.getId())) {
            throw new UnauthorizedException("Operation is not permitted");
        }
    }

    private UUID assertUsername(String username, String domain, UserType type) {
        if (username == null) {
            return null;
        }

        if (type == null) {
            type = UserPrincipal.assertCurrent().getType();
        }

        return userManager.getId(username, domain, type)
                .orElseThrow(() -> new ConcordApplicationException("User not found: " + username));
    }

    private UUID assertUserId(UUID userId) {
        if (userId == null) {
            return null;
        }

        if (userManager.get(userId).isEmpty()) {
            throw new ValidationErrorsException("User not found: " + userId);
        }

        return userId;
    }

    private static String trim(String s) {
        if (s == null) {
            return null;
        }

        return s.trim();
    }

    public static class Dao extends AbstractDao {

        @Inject
        public Dao(@MainDB Configuration cfg) {
            super(cfg);
        }

        public UUID createOrUpdateApiKey(String name, String key, OffsetDateTime expiredAt) {
            return txResult(tx -> {
                tx.deleteFrom(API_KEYS)
                        .where(API_KEYS.KEY_NAME.eq(name))
                        .execute();

                return tx.insertInto(API_KEYS)
                        .columns(API_KEYS.API_KEY, API_KEYS.KEY_NAME, API_KEYS.EXPIRED_AT)
                        .values(hash(key), name, expiredAt)
                        .returning(API_KEYS.KEY_ID)
                        .fetchOne()
                        .getKeyId();
            });
        }
    }
}
