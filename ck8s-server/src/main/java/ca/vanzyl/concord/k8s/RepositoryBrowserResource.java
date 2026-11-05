package ca.vanzyl.concord.k8s;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.walmartlabs.concord.server.org.OrganizationDao;
import com.walmartlabs.concord.server.org.project.ProjectDao;
import com.walmartlabs.concord.server.org.project.RepositoryDao;
import com.walmartlabs.concord.server.repository.RepositoryManager;
import com.walmartlabs.concord.server.sdk.rest.Resource;

import javax.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static java.util.Objects.requireNonNull;
import static jakarta.ws.rs.core.Response.Status.*;

/**
 * Serves static files (form resources, etc.) from project repositories.
 * Files must match the project's {@code _plugin.repository-browser.allowList.<repositoryName>}
 * regexes.
 * <p>
 * Moved from ca.ibodrov.concord:repository-browser-plugin:1.0.1
 * (https://github.com/concord-workflow/repository-browser-plugin, Apache 2.0).
 */
@Path("/api/plugin/repositorybrowser/v1")
public class RepositoryBrowserResource implements Resource {

    private static final Duration DEFAULT_CACHE_EXPIRATION = Duration.ofHours(1);

    private final OrganizationDao organizationDao;
    private final ProjectDao projectDao;
    private final RepositoryDao repositoryDao;
    private final RepositoryManager repositoryManager;
    private final LoadingCache<CacheKey, CacheEntry> cache;

    @Inject
    public RepositoryBrowserResource(OrganizationDao organizationDao,
                                     ProjectDao projectDao,
                                     RepositoryDao repositoryDao,
                                     RepositoryManager repositoryManager) {

        this.organizationDao = requireNonNull(organizationDao);
        this.projectDao = requireNonNull(projectDao);
        this.repositoryDao = requireNonNull(repositoryDao);
        this.repositoryManager = requireNonNull(repositoryManager);
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(DEFAULT_CACHE_EXPIRATION)
                .build(new CacheLoader<>() {
                    @Override
                    public CacheEntry load(CacheKey key) {
                        return fetch(key);
                    }
                });
    }

    @GET
    @Path("/{orgName}/{projectName}/{repositoryName}/{path:.*}")
    public Response serve(@PathParam("orgName") String orgName,
                          @PathParam("projectName") String projectName,
                          @PathParam("repositoryName") String repositoryName,
                          @PathParam("path") String path) {
        try {
            var entry = cache.get(new CacheKey(orgName, projectName, repositoryName, path));
            return Response.ok(entry.content(), entry.mediaType()).build();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof WebApplicationException wae) {
                throw wae;
            }
            throw new WebApplicationException(e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }

    private CacheEntry fetch(CacheKey key) {
        var orgId = organizationDao.getId(key.orgName());
        if (orgId == null) {
            throw new WebApplicationException("Organization not found: %s".formatted(key.orgName()), NOT_FOUND);
        }

        var projectId = projectDao.getId(orgId, key.projectName());
        if (projectId == null) {
            throw new WebApplicationException("Project not found: %s".formatted(key.projectName()), NOT_FOUND);
        }

        var allowList = getAllowList(projectId, key.repositoryName());
        if (allowList.stream().noneMatch(key.path()::matches)) {
            throw new WebApplicationException("Not allowed in the project's (%s) allowList".formatted(projectId),
                    FORBIDDEN);
        }

        var repositoryEntry = repositoryDao.get(projectId, key.repositoryName());
        if (repositoryEntry == null) {
            throw new WebApplicationException("Repository not found: %s".formatted(key.repositoryName()), NOT_FOUND);
        }

        var url = repositoryEntry.getUrl();

        return repositoryManager.withLock(url, () -> {
            var repository = repositoryManager.fetch(projectId, repositoryEntry);

            var src = repository.path().resolve(key.path());
            if (!Files.exists(src)) {
                throw new WebApplicationException("Not found: %s".formatted(key.path()), NOT_FOUND);
            }

            var mediaType = getMediaType(src.getFileName().toString())
                    .orElseThrow(() -> new WebApplicationException("Unsupported file type: %s".formatted(key.path),
                            BAD_REQUEST));

            var content = Files.readAllBytes(src);
            return new CacheEntry(content, mediaType);
        });
    }

    @SuppressWarnings("unchecked")
    private List<String> getAllowList(UUID projectId, String repositoryName) {
        var maybeList = projectDao.getConfigurationValue(projectId, "_plugin", "repository-browser", "allowList",
                repositoryName);

        if (!(maybeList instanceof List<?> list)) {
            throw new WebApplicationException("Invalid allowList value: %s".formatted(maybeList), FORBIDDEN);
        }

        for (int i = 0; i < list.size(); i++) {
            var item = list.get(i);
            if (!(item instanceof String)) {
                throw new WebApplicationException("Invalid allowList item %d: %s".formatted(i, item),
                        INTERNAL_SERVER_ERROR);
            }
        }

        return (List<String>) list;
    }

    record CacheKey(String orgName,
            String projectName,
            String repositoryName,
            String path) {
    }

    record CacheEntry(byte[] content, String mediaType) {
    }

    private static Optional<String> getMediaType(String fileName) {
        var extIdx = fileName.lastIndexOf('.');
        if (extIdx < 2 || extIdx >= fileName.length() - 1) {
            return Optional.empty();
        }
        var ext = fileName.substring(extIdx + 1).toLowerCase();
        return Optional.ofNullable(switch (ext) {
            case "css" -> "text/css";
            case "gif" -> "image/gif";
            case "html" -> "text/html";
            case "jpg", "jpeg" -> "image/jpeg";
            case "js" -> "text/javascript";
            case "png" -> "image/png";
            case "svg" -> "image/svg+xml";
            case "ttf" -> "font/ttf";
            case "webp" -> "image/webp";
            case "woff" -> "font/woff";
            case "woff2" -> "font/woff2";
            default -> null;
        });
    }
}
