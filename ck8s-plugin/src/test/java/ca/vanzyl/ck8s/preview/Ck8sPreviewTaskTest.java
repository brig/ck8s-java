package ca.vanzyl.ck8s.preview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class Ck8sPreviewTaskTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void removesTopLevelField() throws Exception {
        var json = """
            {
              "metadata": {
                "creationTimestamp": "2023-01-01T00:00:00Z",
                "name": "my-resource"
              }
            }
            """;

        var root = (ObjectNode) mapper.readTree(json);
        var pathsToRemove = List.of(
                List.of("metadata", "creationTimestamp")
        );

        Ck8sPreviewTask.removeNestedFields(root, pathsToRemove);

        assertFalse(root.path("metadata").has("creationTimestamp"));
        assertEquals("my-resource", root.path("metadata").path("name").asText());
    }

    @Test
    public void removesDeepNestedFieldWithDotsInKey() throws Exception {
        var json = """
            {
              "metadata": {
                "annotations": {
                  "kubectl.kubernetes.io/last-applied-configuration": "some-config"
                }
              }
            }
            """;

        var root = (ObjectNode) mapper.readTree(json);
        var pathsToRemove = List.of(
                List.of("metadata", "annotations", "kubectl.kubernetes.io/last-applied-configuration")
        );

        Ck8sPreviewTask.removeNestedFields(root, pathsToRemove);

        assertFalse(root.path("metadata").path("annotations").has("kubectl.kubernetes.io/last-applied-configuration"));
    }

    @Test
    public void removesWholeStatusObject() throws Exception {
        var json = """
            {
              "status": {
                "ready": true
              },
              "kind": "Pod"
            }
            """;

        var root = (ObjectNode) mapper.readTree(json);
        var pathsToRemove = List.of(
                List.of("status")
        );

        Ck8sPreviewTask.removeNestedFields(root, pathsToRemove);

        assertFalse(root.has("status"));
        assertEquals("Pod", root.path("kind").asText());
    }

    @Test
    public void removesNestedFieldAndEmptyParents() throws Exception {
        var json = """
            {
              "metadata": {
                "annotations": {
                  "kubectl.kubernetes.io/last-applied-configuration": "value"
                }
              },
              "kind": "ServiceAccount"
            }
            """;

        var root = (ObjectNode) mapper.readTree(json);
        var pathsToRemove = List.of(
                List.of("metadata", "annotations", "kubectl.kubernetes.io/last-applied-configuration")
        );

        Ck8sPreviewTask.removeNestedFields(root, pathsToRemove);

        // The exact key should be removed
        assertFalse(root.at("/metadata/annotations").has("kubectl.kubernetes.io/last-applied-configuration"));

        // The annotations object should be completely gone (because it's empty)
        assertFalse(root.path("metadata").has("annotations"));

        assertFalse(root.has("metadata"));

        // Other data should remain untouched
        assertEquals("ServiceAccount", root.path("kind").asText());
    }

    @Test
    public void removesDeepFieldAndCleansOnlyEmptyParents() throws Exception {
        var json = """
            {
              "metadata": {
                "annotations": {
                  "keep-me": "yes",
                  "kubectl.kubernetes.io/last-applied-configuration": "remove-me"
                }
              }
            }
            """;

        var root = (ObjectNode) mapper.readTree(json);
        var pathsToRemove = List.of(
                List.of("metadata", "annotations", "kubectl.kubernetes.io/last-applied-configuration")
        );

        Ck8sPreviewTask.removeNestedFields(root, pathsToRemove);

        // Removed field
        assertFalse(root.at("/metadata/annotations").has("kubectl.kubernetes.io/last-applied-configuration"));

        // "keep-me" should still be there
        assertEquals("yes", root.at("/metadata/annotations/keep-me").asText());

        // Parent objects are not removed since "annotations" is not empty
        assertTrue(root.has("metadata"));
        assertTrue(root.path("metadata").has("annotations"));
    }
}
