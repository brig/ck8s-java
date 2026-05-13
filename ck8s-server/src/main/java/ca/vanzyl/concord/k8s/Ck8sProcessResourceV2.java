package ca.vanzyl.concord.k8s;

import ca.vanzyl.ck8s.common.MergeUtils;
import ca.vanzyl.concord.k8s.jooq.tables.Ck8sConnectedClusters;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.walmartlabs.concord.common.IOUtils;
import com.walmartlabs.concord.common.TemporaryPath;
import com.walmartlabs.concord.db.AbstractDao;
import com.walmartlabs.concord.db.MainDB;
import com.walmartlabs.concord.repository.FetchResult;
import com.walmartlabs.concord.repository.Repository;
import com.walmartlabs.concord.repository.RepositoryException;
import com.walmartlabs.concord.sdk.Constants;
import com.walmartlabs.concord.sdk.MapUtils;
import com.walmartlabs.concord.sdk.Secret;
import com.walmartlabs.concord.server.org.OrganizationDao;
import com.walmartlabs.concord.server.org.OrganizationManager;
import com.walmartlabs.concord.server.org.project.ProjectDao;
import com.walmartlabs.concord.server.org.secret.SecretManager;
import com.walmartlabs.concord.server.process.*;
import com.walmartlabs.concord.server.process.queue.ProcessQueueManager;
import com.walmartlabs.concord.server.process.state.ProcessStateManager;
import com.walmartlabs.concord.server.repository.RepositoryManager;
import com.walmartlabs.concord.server.sdk.ConcordApplicationException;
import com.walmartlabs.concord.server.sdk.PartialProcessKey;
import com.walmartlabs.concord.server.sdk.ProcessKey;
import com.walmartlabs.concord.server.sdk.metrics.WithTimer;
import com.walmartlabs.concord.server.sdk.rest.Resource;
import com.walmartlabs.concord.server.security.UserPrincipal;
import dev.ybrig.ck8s.cli.common.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.immutables.value.Value;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;

@Deprecated
@Named
@Singleton
@javax.ws.rs.Path("/api/ck8s/v2/process")
@Tag(name = "Ck8s")
public class Ck8sProcessResourceV2 implements Resource {

    private static final Logger log = LoggerFactory.getLogger(Ck8sProcessResourceV2.class);

    private static final String ENTRY_POINT = "normalFlow";

    private final ProcessStateManager stateManager;
    private final ProcessQueueManager processQueueManager;
    private final ProcessManager processManager;
    private final OrganizationDao orgDao;
    private final ProjectDao projectDao;
    private final RepositoryManager repositoryManager;
    private final SecretManager secretManager;
    private final ObjectMapper objectMapper;

    private final ConnectedClustersDao dao;

    @Inject
    public Ck8sProcessResourceV2(ProcessStateManager stateManager,
                                 ProcessQueueManager processQueueManager,
                                 ProcessManager processManager,
                                 OrganizationDao orgDao,
                                 ProjectDao projectDao,
                                 RepositoryManager repositoryManager,
                                 SecretManager secretManager,
                                 ObjectMapper objectMapper,
                                 ConnectedClustersDao dao) {
        this.stateManager = stateManager;
        this.processQueueManager = processQueueManager;
        this.processManager = processManager;
        this.orgDao = orgDao;
        this.projectDao = projectDao;
        this.repositoryManager = repositoryManager;
        this.secretManager = secretManager;
        this.objectMapper = objectMapper;
        this.dao = dao;
    }

    @POST
    @javax.ws.rs.Path("{id}/state")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(description = "Upload process attachments", operationId = "uploadProcessState")
    @RequestBody(description = "Attachment content", required = true,
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM,
                    schema = @Schema(type = "string", format = "binary")
            )
    )
    public void uploadState(@PathParam("id") UUID instanceId, InputStream data) {
        ProcessEntry entry = assertProcess(PartialProcessKey.from(instanceId));
        ProcessKey processKey = new ProcessKey(entry.instanceId(), entry.createdAt());

        Path tmpIn = null;
        Path tmpDir = null;
        try {
            tmpIn = IOUtils.createTempFile("state", ".zip");
            Files.copy(data, tmpIn, StandardCopyOption.REPLACE_EXISTING);

            tmpDir = IOUtils.createTempDir("state");
            IOUtils.unzip(tmpIn, tmpDir);

            Path finalTmpDir = tmpDir;
            stateManager.tx(tx -> {
                stateManager.importPath(tx, processKey, null, finalTmpDir, (p, attrs) -> true);
            });

        } catch (IOException e) {
            log.error("uploadAttachments ['{}'] -> error", processKey, e);
            throw new ConcordApplicationException("upload error: " + e.getMessage());
        } finally {
            if (tmpDir != null) {
                try {
                    IOUtils.deleteRecursively(tmpDir);
                } catch (IOException e) {
                    log.warn("uploadAttachments -> cleanup error: {}", e.getMessage());
                }
            }
            if (tmpIn != null) {
                try {
                    Files.delete(tmpIn);
                } catch (IOException e) {
                    log.warn("uploadAttachments -> cleanup error: {}", e.getMessage());
                }
            }
        }
    }

    @POST
    @javax.ws.rs.Path("debug/{path: .*}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @WithTimer
    public StartProcessResponse startDebug(MultipartInput multipartInput,
                                           @Context HttpServletRequest request) {

        StartProcessRequest input = MultipartStartProcessRequest.from(multipartInput);
        try {
            return start(input, request);
        } finally {
            try {
                multipartInput.close();
            } catch (Exception e) {
                log.warn("startProcess -> multipart close error: {}", e.getMessage());
            }
        }
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @WithTimer
    @Operation(description = "Start ck8s process", operationId = "startProcess")
    public StartProcessResponse start(@Parameter(schema = @Schema(type = "object", implementation = MultipartStartProcessRequest.class)) MultipartInput multipartInput,
                                      @Context HttpServletRequest request) {

        StartProcessRequest input = MultipartStartProcessRequest.from(multipartInput);
        try {
            return start(input, request);
        } finally {
            try {
                multipartInput.close();
            } catch (Exception e) {
                log.warn("startProcess -> multipart close error: {}", e.getMessage());
            }
        }
    }

    public StartProcessResponse start(StartProcessRequest input, HttpServletRequest request) {
        String clientClusterAlias = input.anyClusterAlias();
        ClusterInfo clientClusterInfo = getclusterInfo(clientClusterAlias);
        if (clientClusterInfo == null) {
            clientClusterInfo = getDefaultClusterInfoOrNull();
        }

        UUID orgId = assertOrgId(input);
        UUID projectId = getProject(orgId, input);
        if (projectId == null) {
            projectId = findProjectForClient(orgId, clientClusterInfo);
        }

        try (TemporaryPath tmpPath = IOUtils.tempDir("payload")) {

            Ck8sFlowsInfo ck8s = buildCk8sFlows(input, orgId, projectId, tmpPath.path());

            var clusterRequest = buildClusterRequestOrNull(input, ck8s.ck8sPath());
            var groupClusterRequests = buildGroupClusterRequestsOrNull(ck8s.ck8sPath(), clusterRequest);

            ConcordYaml concordYaml = buildConcordYaml(input, ck8s, clientClusterInfo, clusterRequest, groupClusterRequests);
            concordYaml.write(ck8s.flows().location());

            if (input.hasAdditionalConcordYaml()) {
                try (InputStream is = input.getAdditionalConcordYaml()) {
                    Path dst = ck8s.flows().location().resolve("concord").resolve(input.getAdditionalConcordYamlName());
                    log.info("additional concord yaml {}", dst);
                    Files.copy(is, dst, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            Path archivePath = tmpPath.path().resolve("payload.zip");
            archiveToFile(ck8s.flows().location(), archivePath);

            return start(orgId, projectId, archivePath, input.getParentInstanceId(), input, request);
        } catch (IOException e) {
            log.error("start -> error creating a payload: {}", e.getMessage());
            throw new ConcordApplicationException("Error creating a payload", e);
        }
    }

    private static List<Map<String, Object>> buildGroupClusterRequestsOrNull(Ck8sPath ck8sPath, Map<String, Object> clusterRequest) {
        if (clusterRequest == null) {
            return null;
        }

        var clusterGroup = dev.ybrig.ck8s.cli.common.MapUtils.getString(clusterRequest, "clusterGroup.alias");
        return Ck8sUtils.findClustersYaml(ck8sPath, clusterGroup).stream()
                .map(c -> Ck8sUtils.buildClusterRequest(ck8sPath, c))
                .toList();
    }

    private static Map<String, Object> buildClusterRequestOrNull(StartProcessRequest input, Ck8sPath ck8sPath) {
        if (ck8sPath == null || !input.isBuildClusterRequest()) {
            return null;
        }

        if (input.getClusterAlias() == null) {
            throw new RuntimeException("Can't build cluster request: cluster alias undefined");
        }

        return Ck8sUtils.buildClusterRequest(ck8sPath, input.getClusterAlias());
    }

    private static ConcordYaml buildConcordYaml(StartProcessRequest input, Ck8sFlowsInfo ck8s, ClusterInfo clientClusterInfo, Map<String, Object> clusterRequest, List<Map<String, Object>> groupClusterRequests) {
        Map<String, Object> args = new LinkedHashMap<>();
        if (clusterRequest != null) {
            args.put(DefaultArgs.CLUSTER_REQUEST, clusterRequest);
        }
        Map<String, Object> inputArgs = new LinkedHashMap<>(input.getArguments());
        inputArgs.remove(DefaultArgs.CLUSTER_REQUEST);
        args.put("inputArgs", inputArgs);

        args = dev.ybrig.ck8s.cli.common.MapUtils.merge(args, input.getArguments());

        ImmutableConcordYaml.Builder concordYaml = ConcordYaml.builder()
                .entryPoint(ENTRY_POINT)
                .debug(input.isDebug())
                .requirements(buildRequirements(input, clientClusterInfo))
                .exclusive(input.getExclusiveOptions())
                .arguments(args)
                .putArguments("flow", input.getFlowName())
                .putArguments("ck8sCliVersion", VersionProvider.get())
                .meta(input.getMeta())
                .putMeta("flow", input.getFlowName());

        if (ck8s.ck8sRef() != null) {
            concordYaml.putArguments("ck8sRef", Objects.requireNonNull(ck8s.ck8sRef()));
        }

        if (ck8s.ck8sBranch() != null) {
            concordYaml.putArguments("processCk8sBranch", ck8s.ck8sBranch());
        }

        if (ck8s.ck8sExtRef() != null) {
            concordYaml.putArguments("ck8sExtRef", Objects.requireNonNull(ck8s.ck8sExtRef()));
        }
        if (ck8s.ck8sExtBranch() != null) {
            concordYaml.putArguments("processCk8sExtBranch", ck8s.ck8sExtBranch());
        }

        if (clientClusterInfo != null) {
            concordYaml.putArguments("concordInstanceAlias", clientClusterInfo.clusterAlias())
                    .putArguments("concordInstanceGroupAlias", clientClusterInfo.clusterGroupAlias());
        }

        return concordYaml.build();
    }

    @WithTimer
    private Ck8sFlowsInfo buildCk8sFlows(StartProcessRequest input, UUID orgId, UUID projectId, Path tmpDir) throws IOException {
        Path targetDir = tmpDir.resolve("target");
        Files.createDirectories(targetDir);

        if (input.hasFlowsArchive()) {
            try (InputStream is = input.getFlowsArchive()) {

                IOUtils.unzip(is, targetDir);

                return Ck8sFlowsInfo.builder()
                        .flows(Ck8sFlows.builder()
                                .location(targetDir)
                                .build())
                        .build();
            } catch (Exception e) {
                throw new RuntimeException("Error while processing ck8s flows archive");
            }
        }

        Ck8sRepositoryInfo ck8sInfo = assertCk8sInfo(orgId, projectId, input);

        Path repoDir = tmpDir.resolve("repo");
        Files.createDirectories(repoDir);

        // clone ck8s
        Path ck8s = repoDir.resolve("ck8s");
        FetchResult ck8sFetchResult = fetch(orgId, ck8sInfo.ck8s(), ck8s);

        // clone ck8s-ext
        FetchResult ck8sExtFetchResult = null;
        Path ck8sExt = null;
        if (ck8sInfo.ck8sExt() != null) {
            ck8sExt = repoDir.resolve("ck8sExt");
            ck8sExtFetchResult = fetch(orgId, Objects.requireNonNull(ck8sInfo.ck8sExt()), ck8sExt);
        }

        Ck8sPath ck8sPath = new Ck8sPath(ck8s, ck8sExt);

        Ck8sFlows flows = new Ck8sFlowBuilder(ck8sPath, targetDir, input.anyClusterAlias())
                .includeTests(input.isIncludeTests())
                .build();

        return Ck8sFlowsInfo.builder()
                .ck8sRef(ck8sFetchResult.head())
                .ck8sBranch(ck8sFetchResult.branchOrTag())
                .ck8sExtRef(ck8sExtFetchResult != null ? ck8sExtFetchResult.head() : null)
                .ck8sExtBranch(ck8sExtFetchResult != null ? ck8sExtFetchResult.branchOrTag() : null)
                .flows(flows)
                .ck8sPath(ck8sPath)
                .build();
    }

    private RepositoryInfo ck8sRepo(Map<String, Object> cfg, String name) {
        if (!cfg.containsKey(name)) {
            return null;
        }
        return objectMapper.convertValue(MapUtils.assertMap(cfg, name), RepositoryInfo.class);
    }

    private UUID assertOrgId(StartProcessRequest input) {
        String org = input.getOrg();
        if (org == null) {
            return OrganizationManager.DEFAULT_ORG_ID;
        }
        UUID orgId = orgDao.getId(org);
        if (orgId != null) {
            return orgId;
        }
        throw new ConcordApplicationException("Org '" + org + "' not found");
    }

    private UUID getProject(UUID orgId, StartProcessRequest input) {
        String project = input.getProject();
        if (project == null) {
            return null;
        }
        return projectDao.getId(orgId, project);
    }

    private UUID findProjectForClient(UUID orgId, ClusterInfo clientClusterInfo) {
        if (clientClusterInfo == null) {
            return null;
        }

        String projectName = clientClusterInfo.clusterGroupAlias();
        while (projectName.length() < 3) {
            projectName += "_";
        }

        return projectDao.getId(orgId, projectName);
    }

    private ClusterInfo getclusterInfo(String clusterOrGroupAlias) {
        return dao.findCluster(clusterOrGroupAlias);
    }

    private Ck8sRepositoryInfo assertCk8sInfo(UUID orgId, UUID projectId, StartProcessRequest input) {
        Map<String, Object> cfg = null;
        if (projectId != null) {
            cfg = projectDao.getConfiguration(projectId);
            if (MapUtils.getMap(cfg, "ck8s", Collections.emptyMap()).isEmpty()) {
                cfg = null;
            }
        }
        if (cfg == null) {
            cfg = orgDao.getConfiguration(orgId);
        }
        if (cfg == null) {
            throw new RuntimeException("No ck8s/ck8s-ext configuration found in project '" + projectId + "' or org '" + orgId + "'");
        }

        RepositoryInfo ck8sInfo = ck8sRepo(cfg, "ck8s");
        if (ck8sInfo == null) {
            throw new RuntimeException("Can't find ck8s repository configuration");
        }

        String ck8sRef = input.getCk8sRef();
        if (ck8sRef != null) {
            ck8sInfo = RepositoryInfo.builder().from(ck8sInfo)
                    .ref(ck8sRef)
                    .build();
        }

        RepositoryInfo ck8sExtInfo = ck8sRepo(cfg, "ck8sExt");
        if (ck8sExtInfo != null) {
            String ck8sExtRef = input.getCk8sExtRef();
            if (ck8sExtRef != null) {
                ck8sExtInfo = RepositoryInfo.builder().from(ck8sExtInfo)
                        .ref(ck8sExtRef)
                        .build();
            }
        }

        return Ck8sRepositoryInfo.builder()
                .ck8s(ck8sInfo)
                .ck8sExt(ck8sExtInfo)
                .build();
    }

    private FetchResult fetch(UUID orgId, RepositoryInfo repo, Path dst) {
        return repositoryManager.withLock(repo.url(), () -> {
            try {
                Secret secret = getSecret(orgId, repo.secretName());
                Repository repository = repositoryManager.fetch(repo.url(), repo.ref(), repo.ref(), null, secret, true);

                Files.createDirectories(dst);
                repository.export(dst);

                return repository.fetchResult();
            } catch (Exception e) {
                log.error("process -> repository error", e);
                throw new ConcordApplicationException("Error fetching repository '" + repo.url() + "'", e);
            }
        });
    }

    @WithTimer
    private StartProcessResponse start(UUID orgId, UUID projectId, Path archivePath, UUID parentInstanceId, StartProcessRequest input, HttpServletRequest request) throws IOException {
        UserPrincipal initiator = UserPrincipal.assertCurrent();

        Payload payload;
        try (InputStream archive = Files.newInputStream(archivePath)) {
            payload = PayloadBuilder.start(PartialProcessKey.create())
                    .entryPoint(ENTRY_POINT)
                    .parentInstanceId(parentInstanceId)
                    .organization(orgId)
                    .project(projectId)
                    .initiator(initiator.getId(), initiator.getUsername())
                    .request(request)
                    .workspace(archive)
                    .activeProfiles(input.getActiveProfiles())
                    .outExpressions(input.getOutExpressions().toArray(new String[0]))
                    .configuration(Map.of("out", input.getOutExpressions(), Constants.Request.DRY_RUN_MODE_KEY, input.isDryRun()))
                    .build();

            Map<String, Path> attachments = new HashMap<>();
            for (Map.Entry<String, InputPart> e : input.getAttachments().entrySet()) {
                String name = e.getKey();
                try (InputStream in = e.getValue().getBody(InputStream.class, null)) {
                    Path baseDir = payload.getHeader(Payload.BASE_DIR);
                    Path dst = baseDir.resolve(e.getKey());
                    Files.createDirectories(dst.getParent());
                    try (OutputStream out = Files.newOutputStream(dst)) {
                        IOUtils.copy(in, out);
                    }

                    attachments.put(name, dst);
                }
            }

            payload = payload.putAttachments(attachments);
        } catch (IOException e) {
            log.error("start -> error creating a payload: {}", e.getMessage());
            throw new ConcordApplicationException("Error creating a payload", e);
        }

        return new StartProcessResponse(processManager.start(payload).getInstanceId());
    }

    private Secret getSecret(UUID orgId, String secretName) {
        if (secretName == null) {
            return null;
        }

        SecretManager.DecryptedSecret s = secretManager.getSecret(SecretManager.AccessScope.internal(), orgId, secretName, null, null);
        if (s == null) {
            throw new RepositoryException("Secret not found: " + secretName);
        }

        return s.getSecret();
    }

    @WithTimer
    private static void archiveToFile(Path src, Path dest) {
        try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(Files.newOutputStream(dest, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            IOUtils.zip(zip, src);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> buildRequirements(StartProcessRequest input, ClusterInfo clientClusterInfo) {
        Map<String, Object> requirements = input.getRequirements();
        if (clientClusterInfo == null) {
            return requirements;
        }

        String clusterAlias = input.anyClusterAlias();
        if (!clusterAlias.equals(clientClusterInfo.clusterAlias()) && !clusterAlias.equals(clientClusterInfo.clusterGroupAlias())) {
            clusterAlias = clientClusterInfo.clusterGroupAlias();
        }

        return MergeUtils.merge(requirements, Collections.singletonMap("agent", Collections.singletonMap("clusterAlias", clusterAlias)));
    }

    private static ClusterInfo getDefaultClusterInfoOrNull() {
        if (System.getenv("CONCORD_INSTANCE_ALIAS") == null || System.getenv("CONCORD_INSTANCE_GROUP_ALIAS") == null) {
            return null;
        }

        return ClusterInfo.builder()
                .clusterAlias(System.getenv("CONCORD_INSTANCE_ALIAS"))
                .clusterGroupAlias(System.getenv("CONCORD_INSTANCE_GROUP_ALIAS"))
                .isActive(true)
                .build();
    }

    private ProcessEntry assertProcess(PartialProcessKey processKey) {
        ProcessEntry p = processQueueManager.get(processKey);
        if (p == null) {
            throw new ConcordApplicationException("Process instance not found", Response.Status.NOT_FOUND);
        }
        return p;
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonSerialize(as = ImmutableRepositoryInfo.class)
    @JsonDeserialize(as = ImmutableRepositoryInfo.class)
    interface RepositoryInfo {

        String url();

        String ref();

        String secretName();

        static ImmutableRepositoryInfo.Builder builder() {
            return ImmutableRepositoryInfo.builder();
        }
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    interface Ck8sFlowsInfo {

        @Nullable
        String ck8sRef();

        @Nullable
        String ck8sBranch();

        @Nullable
        String ck8sExtRef();

        @Nullable
        String ck8sExtBranch();

        Ck8sFlows flows();

        @Nullable
        Ck8sPath ck8sPath();

        static ImmutableCk8sFlowsInfo.Builder builder() {
            return ImmutableCk8sFlowsInfo.builder();
        }
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    interface Ck8sRepositoryInfo {

        RepositoryInfo ck8s();

        @Nullable
        RepositoryInfo ck8sExt();

        static ImmutableCk8sRepositoryInfo.Builder builder() {
            return ImmutableCk8sRepositoryInfo.builder();
        }
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonSerialize(as = ImmutableClusterInfo.class)
    @JsonDeserialize(as = ImmutableClusterInfo.class)
    public interface ClusterInfo {

        String clusterAlias();

        String clusterGroupAlias();

        boolean isActive();

        static ImmutableClusterInfo.Builder builder() {
            return ImmutableClusterInfo.builder();
        }
    }

    @Named
    public static class ConnectedClustersDao extends AbstractDao {

        @Inject
        public ConnectedClustersDao(@MainDB Configuration cfg) {
            super(cfg);
        }

        public ClusterInfo findCluster(String clusterOrGroupAlias) {
            return txResult(tx -> findCluster(tx, clusterOrGroupAlias));
        }

        public ClusterInfo findCluster(DSLContext tx, String clusterOrGroupAlias) {
            Ck8sConnectedClusters t = Ck8sConnectedClusters.CK8S_CONNECTED_CLUSTERS;
            return tx.select(t.CLUSTER_ALIAS, t.CLUSTER_GROUP_ALIAS, t.IS_ACTIVE)
                    .from(t)
                    .where(t.CLUSTER_ALIAS.eq(clusterOrGroupAlias)
                            .or(t.CLUSTER_GROUP_ALIAS.eq(clusterOrGroupAlias).and(t.IS_ACTIVE.eq(true))))
                    .limit(1)
                    .fetchOne(r -> ClusterInfo.builder()
                            .clusterAlias(r.get(t.CLUSTER_ALIAS))
                            .clusterGroupAlias(r.get(t.CLUSTER_GROUP_ALIAS))
                            .isActive(r.get(t.IS_ACTIVE))
                            .build());
        }

        public void registerCluster(ClusterInfo clusterInfo) {
            tx(tx -> registerCluster(tx, clusterInfo));
        }

        public void registerCluster(DSLContext tx, ClusterInfo clusterInfo) {
            Ck8sConnectedClusters t = Ck8sConnectedClusters.CK8S_CONNECTED_CLUSTERS;
            tx.insertInto(t)
                    .columns(t.CLUSTER_ALIAS, t.CLUSTER_GROUP_ALIAS)
                    .values(clusterInfo.clusterAlias(), clusterInfo.clusterGroupAlias())
                    .onDuplicateKeyUpdate()
                    .set(t.IS_ACTIVE, clusterInfo.isActive())
                    .execute();
        }

        public void unregisterCluster(String alias) {
            tx(tx -> unregisterCluster(tx, alias));
        }

        public void unregisterCluster(DSLContext tx, String alias) {
            Ck8sConnectedClusters t = Ck8sConnectedClusters.CK8S_CONNECTED_CLUSTERS;
            tx.deleteFrom(t)
                    .where(t.CLUSTER_ALIAS.eq(alias))
                    .execute();
        }
    }
}
