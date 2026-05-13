package ca.vanzyl.concord.k8s;

import com.walmartlabs.concord.repository.Snapshot;
import com.walmartlabs.concord.server.org.OrganizationDao;
import com.walmartlabs.concord.server.process.Payload;
import com.walmartlabs.concord.server.sdk.process.CustomEnqueueProcessor;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ca.vanzyl.concord.k8s.PayloadUtils.isCk8sProcess;

public class Ck8sStateProcessor implements CustomEnqueueProcessor {

    private final OrganizationDao organizationDao;

    @Inject
    public Ck8sStateProcessor(OrganizationDao organizationDao) {
        this.organizationDao = organizationDao;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Payload handleState(Payload payload) {
        if (!isCk8sProcess(payload)) {
            return payload;
        }
        var targetCluster = PayloadUtils.clientCluster(payload);
        var orgId = payload.getHeader(Payload.ORGANIZATION_ID);
        var cfg = Optional.ofNullable(organizationDao.getConfiguration(orgId)).orElse(Map.of());
        var clustersWithPayloadArchive = (List<String>)cfg.get("clustersWithPayloadArchive");
        if (clustersWithPayloadArchive == null || !clustersWithPayloadArchive.contains(targetCluster)) {
            return payload;
        }

        var workspace = payload.getHeader(Payload.WORKSPACE_DIR);
        var snapshots = new ArrayList<>(payload.getHeader(Payload.REPOSITORY_SNAPSHOT, List.of()));
        snapshots.add(Snapshot.singleFile(workspace.resolve("concord.yml")));
        snapshots.add(new Ck8sSnapshot(workspace.resolve("ck8s-components")));
        snapshots.add(new Ck8sSnapshot(workspace.resolve("ck8s-components-tests")));
        snapshots.add(new Ck8sSnapshot(workspace.resolve("ck8s-orgs")));
        snapshots.add(new Ck8sSnapshot(workspace.resolve("ck8s-configs")));
        snapshots.add(new Ck8sSnapshot(workspace.resolve("configs")));
        return payload.putHeader(Payload.REPOSITORY_SNAPSHOT, snapshots);
    }

    private static class Ck8sSnapshot implements Snapshot {

        private static final String[] FILE_IGNORE_PATTERNS = new String[]{".*\\.pdf$", ".*\\.png$", ".*\\.jpg$"};

        private final Path directory;

        private Ck8sSnapshot(Path directory) {
            this.directory = directory;
        }

        @Override
        public boolean isModified(Path path, BasicFileAttributes basicFileAttributes) {
            return true;
        }

        @Override
        public boolean contains(Path path) {
            if (matches(path, FILE_IGNORE_PATTERNS)) {
                return false;
            }

            return path.startsWith(directory);
        }

        public static boolean matches(Path p, String... filters) {
            String n = p.getName(p.getNameCount() - 1).toString();
            for (String f : filters) {
                if (n.matches(f)) {
                    return true;
                }
            }
            return false;
        }
    }
}
