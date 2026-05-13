package ca.vanzyl.ck8s;

import ca.vanzyl.ck8s.common.Mapper;
import com.walmartlabs.concord.runtime.v2.runner.SensitiveDataHolder;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class InterpolateTaskTest
{

    private static Path file(String name)
            throws Exception
    {
        var resource = InterpolateTaskTest.class.getResource(name);
        assertNotNull(resource);

        var dest = Files.createTempFile(Paths.get(name).getFileName().toString(), "");
        Files.copy(Paths.get(resource.toURI()), dest, StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }

    @Test
    public void testEscapeExpressions()
            throws Exception
    {
        var inputFile = "/interpolateTask/escape.ini";
        var expected = "/interpolateTask/escape-expected.ini";
        Map<String, Object> vars = Collections.singletonMap("urlValue", "url-value");
        Map<String, Object> input = new HashMap<>();

        assertInterpolate(inputFile, vars, input, expected);
    }

    @Test
    public void testEscapePattern()  throws Exception {
        var inputFile = "/interpolateTask/env.ini";
        var expected = "/interpolateTask/env-expected.ini";
        Map<String, Object> vars = new HashMap<>();
        vars.put("secrets", Collections.singletonMap("myPassword", "123456"));
        vars.put("clusterRequest", Collections.singletonMap("alias", "BOO"));

        Map<String, Object> input = new HashMap<>();
        input.put("debug", true);
        input.put("strict", true);

        assertInterpolate(inputFile, vars, input, expected);
    }

    @Test
    public void testRemoveNullValueLines() throws Exception {
        var inputFile = "/interpolateTask/test.yaml";
        var expected = "/interpolateTask/test-expected.yaml";

        Map<String, Object> args = new HashMap<>();
        args.put("uiUserPoolClientName", null);
        args.put("name", "test");
        args.put("clientId", "test-client");

        Map<String, Object> input = new HashMap<>();
        input.put("debug", true);
        input.put("strict", false);
        input.put("removeEmptyValueLines", true);
        input.put("recursively", true);
        input.put("args", args);

        assertInterpolate(inputFile, Map.of(), input, expected);
    }

    @Test
    public void testRemoveEmptyValueLines() throws Exception {
        var inputFile = "/interpolateTask/test.yaml";
        var expected = "/interpolateTask/test-expected.yaml";

        Map<String, Object> args = new HashMap<>();
        args.put("uiUserPoolClientName", "");
        args.put("name", "test");
        args.put("clientId", "test-client");

        Map<String, Object> input = new HashMap<>();
        input.put("debug", true);
        input.put("strict", false);
        input.put("removeEmptyValueLines", true);
        input.put("args", args);

        assertInterpolate(inputFile, Map.of(), input, expected);
    }

    @Test
    public void interpolateKeys() throws Exception {
        var inputFile = "/interpolateTask/keys-with-expr.json";
        var expected = "/interpolateTask/keys-with-expr-expected.json";

        Map<String, Object> args = new HashMap<>();
        args.put("name", "test");
        args.put("testClient", "test-client");
        args.put("client", Map.of("id", "${testClient}"));

        Map<String, Object> input = new HashMap<>();
        input.put("debug", true);
        input.put("fileFormat", "json");
        input.put("recursively", true);
        input.put("args", args);

        assertInterpolate(inputFile, Map.of(), input, expected);
    }

    @Test
    public void interpolateRecursively() throws Exception {
        var inputFile = "/interpolateTask/aep.yaml";
        var expected = "/interpolateTask/aep-expected.yaml";

        Map<String, Object> args = new HashMap<>();
        args.put("clusterRequest", Map.of("domain", "local", "key", "value"));
        args.put("aepId", "test-aep");
        args.put("client", Map.of("id", "${testClient}"));
        args.put("aep", Mapper.yaml().readMap(file(inputFile).toAbsolutePath()).get("aep"));

        Map<String, Object> input = new HashMap<>();
        input.put("debug", true);
        input.put("fileFormat", "yaml");
        input.put("recursively", true);
        input.put("args", args);

        assertInterpolate(inputFile, Map.of(), input, expected);
    }

    @Test
    public void evalMap() {
        var task = new InterpolateTask(new MockTestContext(Map.of()), null);
        var m = Map.of("kubernetes.io/cluster/${clusterName}", "owned");

        var result = task.evalMap(m, Map.of("clusterName", "tst"));
        System.out.println(result);
    }

    @Test
    public void testMask() throws Exception {
        var inputFile = "/interpolateTask/mask.yaml";
        var expected = "/interpolateTask/mask-expected.yaml";

        Map<String, Object> args = new HashMap<>();
        args.put("aepId", "test-aep");

        Map<String, Object> input = new HashMap<>();
        input.put("debug", true);
        input.put("recursively", true);
        input.put("args", args);
        input.put("maskSensitiveData", true);

        var sensitiveData = Set.of("DEFAULT", "one", "two");

        assertInterpolate(sensitiveData, inputFile, Map.of(), input, expected);
    }

    private static void assertInterpolate(Set<String> sensitiveData, String inputFile, Map<String, Object> variables, Map<String, Object> taskInput, String expectedFile) throws Exception{
        var file = file(inputFile).toAbsolutePath();
        var sensitiveDataHolder = new SensitiveDataHolder();
        sensitiveDataHolder.addAll(sensitiveData);
        var task = new InterpolateTask(new MockTestContext(variables), sensitiveDataHolder);

        taskInput.put("file", file.toString());

        var result = (TaskResult.SimpleResult) task.execute(new MapBackedVariables(taskInput));
        assertTrue(result.ok());

        assertEquals(new String(Files.readAllBytes(file(expectedFile))).trim(), new String(Files.readAllBytes(file)).trim());
    }

    private static void assertInterpolate(String inputFile, Map<String, Object> variables, Map<String, Object> taskInput, String expectedFile) throws Exception {
        assertInterpolate(Set.of(), inputFile, variables, taskInput, expectedFile);
    }
}
