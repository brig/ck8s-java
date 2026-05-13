package ca.vanzyl.ck8s;

import ca.vanzyl.concord.k8s.ImmutablesYamlMapper;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static com.walmartlabs.concord.runtime.v2.sdk.TaskResult.SimpleResult;
import static org.junit.Assert.*;

public class MergeTaskTest
{

    private final MergeTask task = new MergeTask(new MockTestContext(Map.of()));

    private static String resource(String name)
    {
        String resourceName = "/yq/merge/" + name;
        URL resource = MergeTaskTest.class.getResource(resourceName);
        assertNotNull("can't find resource: '" + resourceName + "'", resource);

        return resource.getPath();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readFile(String fileName)
    {
        try {
            return new ImmutablesYamlMapper().read(new File(fileName), Map.class);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void assertContent(TaskResult result, String expectedContentFileName)
    {
        assertContent(result, readFile(resource(expectedContentFileName)));
    }

    @SuppressWarnings("unchecked")
    private static void assertContent(TaskResult result, Map<String, Object> expected)
    {
        assertTrue(result instanceof SimpleResult);

        SimpleResult simple = (TaskResult.SimpleResult) result;
        Map<String, Object> actualContent = (Map<String, Object>) simple.values().get("content");
        assertEquals(expected, actualContent);
    }

    @Test
    public void testMerge0()
    {
        TaskResult result = execute(Map.of("files", List.of(resource("000.a.yml"), resource("000.b.yml"))));
        assertContent(result, "000.result.yml");
    }

    @Test
    public void testMerge1()
    {
        TaskResult result = execute(Map.of("files", List.of(resource("001.a.yml"), resource("001.b.yml"))));
        assertContent(result, "001.result.yml");
    }

    @Test
    public void testMerge2()
    {
        TaskResult result = execute(Map.of("files", List.of(resource("002.a.yml"), resource("002.b.yml"))));
        assertContent(result, "002.result.yml");
    }

    @Test
    public void testMerge3()
    {
        List<String> files = List.of(resource("002.a.yml"), resource("002.b.yml"));
        List<Map<String, Object>> objects = List.of(Map.of("ok", "ov"));

        TaskResult result = execute(Map.of("files", files, "objects", objects));
        assertContent(result, Map.of("arr", List.of(3, 4, 5), "ok", "ov"));
    }

    @Test
    public void testMerge4()
    {
        String dest = Paths.get(System.getProperty("java.io.tmpdir")).resolve("concord.test.yml").toAbsolutePath().toString();
        List<String> files = List.of(resource("002.a.yml"), resource("002.b.yml"));

        execute(Map.of("files", files, "dest", dest));
        assertEquals(Map.of("arr", List.of(3, 4, 5)), readFile(dest));
    }

    @Test
    public void testMerge5()
    {
        Map<String, Object> o1 = Map.of("k", "v");
        Map<String, Object> o2 = Map.of("k1", "v1", "k2", "v2");

        TaskResult result = execute(Map.of("objects", List.of(o1, o2)));
        assertContent(result, Map.of("k", "v", "k1", "v1", "k2", "v2"));
    }

    @Test
    public void testMerge6()
    {
        Map<String, Object> o1 = Map.of("k", Map.of("kk", "vv"));
        Map<String, Object> o2 = Map.of("k", Map.of("kk", List.of("vv2")));

        TaskResult result = execute(Map.of("objects", List.of(o1, o2)));
        assertContent(result, Map.of("k", Map.of("kk", List.of("vv2"))));
    }

    @Test
    public void testArraysMerge() {
        List<Map<String, Object>> a = List.of(Map.of("artifact", "art1", "version", "1.0.0"));
        List<Map<String, Object>> b = List.of(Map.of("artifact", "art2", "version", "1.0.0"), Map.of("artifact", "art1", "version", "1.1.1"));

        List<Map<String, Object>> result = task.arrays(a, b, "artifact");
        assertEquals(2, result.size());
        assertEquals(Map.of("artifact", "art1", "version", "1.1.1"), result.get(0));
        assertEquals(Map.of("artifact", "art2", "version", "1.0.0"), result.get(1));
    }

    private TaskResult execute(Map<String, Object> input)
    {
        return task.execute(new MapBackedVariables(input));
    }
}
