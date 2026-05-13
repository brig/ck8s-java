package ca.vanzyl.ck8s.preview;

import ca.vanzyl.ck8s.actions.DryRunPhases;
import ca.vanzyl.ck8s.common.MapUtils;
import ca.vanzyl.ck8s.common.Mapper;
import ca.vanzyl.ck8s.state.EntityState;
import ca.vanzyl.ck8s.state.MapEntity;
import ca.vanzyl.ck8s.state.MapEntityKey;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.DryRunReady;
import com.walmartlabs.concord.runtime.v2.sdk.ProcessConfiguration;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Named("ck8sPreview")
@DryRunReady
public class Ck8sPreviewTask implements Task {

    private final static Logger log = LoggerFactory.getLogger(Ck8sPreviewTask.class);

    private static final List<List<String>> K8S_ENTITY_KEYS_TO_IGNORE = List.of(
            List.of("metadata", "creationTimestamp"),
            List.of("metadata", "resourceVersion"),
            List.of("metadata", "uid"),
            List.of("metadata", "generation"),
            List.of("metadata", "selfLink"),
            List.of("metadata", "managedFields"),
            List.of("metadata", "annotations", "kubectl.kubernetes.io/last-applied-configuration"),
            List.of("status")
    );

    private final PreviewChangesRecorder preview;
    private final ProcessConfiguration processConfiguration;
    private final EntityState state;
    private final Path workDir;

    @Inject
    public Ck8sPreviewTask(Context context, PreviewChangesRecorder preview, ProcessConfiguration processConfiguration, EntityState state) {
        this.preview = preview;
        this.processConfiguration = processConfiguration;
        this.state = state;
        this.workDir = context.workingDirectory();
    }

    public void change(String id, String type, String action, String name) {
        if (!DryRunPhases.isPreview(processConfiguration)) {
            return;
        }

        preview.record(r -> r.action(Change.Action.valueOf(action))
                .id(id)
                .type(type)
                .metadata(Change.Metadata.builder().name(name).build()));
    }

    public void change(String parentId, String id, String type, String action, String name) {
        if (!DryRunPhases.isPreview(processConfiguration)) {
            return;
        }

        preview.record(r -> r.action(Change.Action.valueOf(action))
                .parentId(parentId)
                .id(id)
                .type(type)
                .metadata(Change.Metadata.builder().name(name).build()));
    }

    public void k8sResource(String oldManifestPath, String newManifestPath) {
        if ((oldManifestPath == null || oldManifestPath.isBlank())
                && (newManifestPath == null || newManifestPath.isBlank())) {
            log.warn("old manifest and new manifest are both blank");
            return;
        }

        var oldManifestObject = readObjectFromYaml(workDir, oldManifestPath, K8S_ENTITY_KEYS_TO_IGNORE);
        var newManifestObject = readObjectFromYaml(workDir, newManifestPath, K8S_ENTITY_KEYS_TO_IGNORE);
        if (oldManifestObject == null && newManifestObject == null) {
            return;
        }
        var anyManifest = newManifestObject != null ? newManifestObject : oldManifestObject;
        var namespace = MapUtils.getString(anyManifest, "metadata.namespace", "");
        var name = MapUtils.getString(anyManifest, "metadata.name");
        var kind = MapUtils.getString(anyManifest, "kind");
        var entityId = "k8s:" + namespace + ":" + kind + ":" + name;
        var entityType = "k8s:" + kind;
        var entityName = (isBlank(namespace) ? "" : namespace + ":") + name;

        state.put(new MapEntityKey(entityId, entityType), oldManifestObject != null ? new MapEntity(entityName, oldManifestObject) : null);
        state.put(new MapEntityKey(entityId, entityType), newManifestObject != null ? new MapEntity(entityName, newManifestObject) : null);
    }

    public void k8sResource(String id, String type, String name, String path) {
        state.put(new MapEntityKey(id, type), readEntityFromYaml(workDir, name, path, K8S_ENTITY_KEYS_TO_IGNORE));
    }

    public void entityFromYaml(String id, String type, String name, String path) {
        state.put(new MapEntityKey(id, type), readEntityFromYaml(workDir, name, path, List.of()));
    }

    private static MapEntity readEntityFromYaml(Path workDir, String name, String path, List<List<String>> keysToRemove) {
        var object = readObjectFromYaml(workDir, path, keysToRemove);
        if (object == null) {
            return null;
        }
        return new MapEntity(name, object);
    }

    private static Map<String, Object> readObjectFromYaml(Path workDir, String path, List<List<String>> keysToRemove) {
        if (path == null || path.isBlank()) {
            return null;
        }

        var p = normalize(workDir, path);
        if (!Files.exists(p)) {
            return null;
        }

        try {
            if (Files.size(p) == 0) {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var root = Mapper.yaml().readNode(p);
        removeEmpty(root);
        removeNestedFields(root, keysToRemove);
        return Mapper.yaml().convertToMap(root);
    }

    private static Path normalize(Path workDir, String path) {
        var p = Path.of(path);
        if (p.startsWith(workDir)) {
            return p.toAbsolutePath();
        }
        return workDir.resolve(p).toAbsolutePath();
    }

    private static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    static void removeNestedFields(JsonNode root, List<List<String>> pathsToRemove) {
        if (!(root instanceof ObjectNode o)) {
            return;
        }

        for (var path : pathsToRemove) {
            var parents = new ArrayList<ObjectNode>();
            var current = o;
            for (var i = 0; i < path.size() - 1; i++) {
                var next = current.get(path.get(i));
                if (!(next instanceof ObjectNode nextObjectNode)) {
                    current = null;
                    break;
                }
                parents.add(current);
                current = nextObjectNode;
            }

            if (current != null) {
                current.remove(path.get(path.size() - 1));

                for (var i = path.size() - 2; i >= 0; i--) {
                    var parent = parents.get(i);
                    var key = path.get(i);

                    var maybeEmpty = parent.get(key);
                    if (maybeEmpty instanceof ObjectNode emptyNode && emptyNode.isEmpty()) {
                        parent.remove(key);
                    } else {
                        break;
                    }
                }
            }
        }
    }

    static JsonNode removeEmpty(JsonNode node) {
        if (node.isObject()) {
            var obj = (ObjectNode) node;
            List<String> toRemove = new ArrayList<>();
            obj.fields().forEachRemaining(entry -> {
                var cleaned = removeEmpty(entry.getValue());
                if (isEmpty(cleaned)) {
                    toRemove.add(entry.getKey());
                } else {
                    obj.set(entry.getKey(), cleaned);
                }
            });
            toRemove.forEach(obj::remove);
            return obj;
        } else if (node.isArray()) {
            var array = (ArrayNode) node;
            var cleanedArray = array.arrayNode();
            for (var item : array) {
                var cleanedItem = removeEmpty(item);
                if (!isEmpty(cleanedItem)) {
                    cleanedArray.add(cleanedItem);
                }
            }
            return cleanedArray;
        } else {
            return node;
        }
    }

    private static boolean isEmpty(JsonNode node) {
        return node == null
                || node.isNull()
                || (node.isObject() && node.isEmpty())
                || (node.isArray() && node.isEmpty());
    }
}
