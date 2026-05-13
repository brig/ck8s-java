package ca.vanzyl.ck8s.preview;

import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;

import static ca.vanzyl.ck8s.asserts.AssertsTask.assertTrue;
import static org.junit.Assert.assertEquals;

public class PlainTextReportWriterTest {

    @Test
    public void test() throws Exception {
        var processInfo = ProcessInfo.builder()
                .cluster("test")
                .flow("myFlow")
                .build();

        var changes = List.<Change>of(
                Change.builder()
                        .id("1")
                        .type("k8s:create-namespace")
                        .metadata(Change.Metadata.builder().name("brig").build())
                        .action(Change.Action.CREATE)
                        .build(),
                Change.builder()
                        .id("2")
                        .parentId("1")
                        .type("k8s:create-label")
                        .metadata(Change.Metadata.builder().name("key2").build())
                        .action(Change.Action.CREATE)
                        .build(),
                Change.builder()
                        .id("3")
                        .parentId("1")
                        .type("k8s:create-label")
                        .metadata(Change.Metadata.builder().name("key3").build())
                        .action(Change.Action.CREATE)
                        .build(),
                Change.builder()
                        .id("4")
                        .type("k8s:create-label")
                        .metadata(Change.Metadata.builder().name("key4").build())
                        .action(Change.Action.CREATE)
                        .build(),
                Change.builder()
                        .id("4.1")
                        .parentId("4")
                        .type("k8s:create-inner")
                        .metadata(Change.Metadata.builder().name("key4.1").build())
                        .action(Change.Action.CREATE)
                        .build(),
                Change.builder()
                        .id("4.1.1")
                        .parentId("4.1")
                        .type("k8s:create-inner")
                        .metadata(Change.Metadata.builder().name("key4.1.1").build())
                        .action(Change.Action.CREATE)
                        .build()
        );

        var w = new PlainTextReportWriter(processInfo, changes);
        try (var out = new ByteArrayOutputStream()) {
            w.write(out);
            System.out.println(out);
        }
    }

    @Test
    public void testSingleCreate() {
        var input = List.of(change("1", Change.Action.CREATE, 100));
        var output = PlainTextReportWriter.normalizeChanges(input);

        assertEquals(1, output.size());
        assertEquals(Change.Action.CREATE, output.get(0).action());
    }

    @Test
    public void testCreateUpdate() {
        var input = List.of(
                change("1", Change.Action.CREATE, 100),
                change("1", Change.Action.UPDATE, 200)
        );
        var output = PlainTextReportWriter.normalizeChanges(input);

        assertEquals(1, output.size());
        assertEquals(Change.Action.CREATE, output.get(0).action());
        assertEquals(200, output.get(0).timestamp().getEpochSecond());
    }

    @Test
    public void testCreateUpdateDelete() {
        var input = List.of(
                change("1", Change.Action.CREATE, 100),
                change("1", Change.Action.UPDATE, 200),
                change("1", Change.Action.DELETE, 300)
        );
        var output = PlainTextReportWriter.normalizeChanges(input);

        // Should be removed
        assertEquals(0, output.size());
    }

    @Test
    public void testUpdateDelete() {
        var input = List.of(
                change("1", Change.Action.UPDATE, 100),
                change("1", Change.Action.DELETE, 200)
        );
        var output = PlainTextReportWriter.normalizeChanges(input);

        assertEquals(1, output.size());
        assertEquals(Change.Action.DELETE, output.get(0).action());
    }

    @Test
    public void testMultipleUpdates() {
        var input = List.of(
                change("1", Change.Action.UPDATE, 100),
                change("1", Change.Action.UPDATE, 200),
                change("1", Change.Action.UPDATE, 300)
        );
        var output = PlainTextReportWriter.normalizeChanges(input);

        assertEquals(1, output.size());
        assertEquals(Change.Action.UPDATE, output.get(0).action());
        assertEquals(300, output.get(0).timestamp().getEpochSecond());
    }

    @Test
    public void testCreateDelete() {
        var input = List.of(
                change("1", Change.Action.CREATE, 100),
                change("1", Change.Action.DELETE, 200)
        );
        var output = PlainTextReportWriter.normalizeChanges(input);

        // Should be removed
        assertEquals(0, output.size());
    }

    @Test
    @Ignore
    public void testCreateDeleteWithChildren() {
        var input = List.of(
                change("1", Change.Action.CREATE, 100),
                child("1", "1.1", Change.Action.CREATE, 101),
                change("1", Change.Action.DELETE, 200)
        );
        var output = PlainTextReportWriter.normalizeChanges(input);

        // Should be removed
        assertEquals(0, output.size());
    }

    @Test
    public void testMultipleIds() {
        var input = List.of(
                change("1", Change.Action.CREATE, 100),
                change("1", Change.Action.UPDATE, 200),
                change("2", Change.Action.UPDATE, 150),
                change("2", Change.Action.DELETE, 250),
                change("3", Change.Action.CREATE, 300)
        );
        var output = PlainTextReportWriter.normalizeChanges(input);

        assertEquals(3, output.size());

        assertTrue(output.stream().anyMatch(c -> c.id().equals("1") && c.action() == Change.Action.CREATE));
        assertTrue(output.stream().anyMatch(c -> c.id().equals("2") && c.action() == Change.Action.DELETE));
        assertTrue(output.stream().anyMatch(c -> c.id().equals("3") && c.action() == Change.Action.CREATE));
    }

    private static Change change(String id, Change.Action action, long timestamp) {
        return Change.builder()
                .id(id)
                .type("testType")
                .action(action)
                .timestamp(Instant.ofEpochSecond(timestamp))
                .build();
    }

    private static Change child(String parentId, String id, Change.Action action, long timestamp) {
        return Change.builder()
                .id(id)
                .parentId(parentId)
                .type("testType")
                .action(action)
                .timestamp(Instant.ofEpochSecond(timestamp))
                .build();
    }
}
