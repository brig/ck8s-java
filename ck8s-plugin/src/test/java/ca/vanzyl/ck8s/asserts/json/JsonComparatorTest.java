package ca.vanzyl.ck8s.asserts.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class JsonComparatorTest
{

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final JsonComparator comparator = new JsonComparator();

    private static Path resourcesAsFile(String name)
            throws Exception
    {
        URL resource = JsonComparatorTest.class.getResource(name);
        assertNotNull(resource);
        return Path.of(resource.toURI());
    }

    @Test
    public void testString()
            throws Exception
    {
        testPass("\"String\"", "\"String\"");
        testFail("\"String\"", "\"String123\"",
                "Expected value at '' to be 'String', but is 'String123'");
    }

    @Test
    public void testObjects()
            throws Exception
    {
        testPass("{\"id\":1}", "{\"id\":1}");
        testFail("{\"id\":1}", "{\"id\":2}", "Expected value at '/id' to be '1', but is '2'");

        testPass("{\"id\":1}", "{\"id\":1, \"name\":\"Sherlock\"}");
        testPass("{\"id\":1, \"name\":\"Sherlock\"}", "{\"id\":1, \"name\":\"Sherlock\"}");

        testFail("{\"id\":1, \"name\":\"Sherlock\"}", "{\"id\":1, \"name\":\"Sherlock2\"}",
                "Expected value at '/name' to be 'Sherlock', but is 'Sherlock2'");
    }

    @Test
    public void testReversed()
            throws Exception
    {
        testPass("{\"name\":\"Sherlock\",\"id\":1}", "{\"id\":1,\"name\":\"Sherlock\"}");
    }

    @Test
    public void testNested()
            throws Exception
    {
        testPass("{\"id\":1, \"address\":{\"addr1\":\"123 Main\", \"addr2\":null, \"city\":\"N\", \"state\":\"X\"}}",
                "{\"id\":1,\"address\":{\"addr1\":\"123 Main\", \"addr2\":null, \"city\":\"N\", \"state\":\"X\"}}");

        testFail("{\"id\":1, \"address\":{\"addr1\":\"123 Main\", \"addr2\":null, \"city\":\"N\", \"state\":\"N\"}}",
                "{\"id\":1, \"address\":{\"addr1\":\"123 Main\", \"addr2\":null, \"city\":\"NN\", \"state\":\"TX\"}}",
                "Expected value at '/address/city' to be 'N', but is 'NN'");
    }

    @Test
    public void testVeryNested()
            throws Exception
    {
        testPass("{\"a\":{\"b\":{\"c\":{\"d\":{\"e\":{\"f\":{\"g\":{\"h\":{\"i\":{\"j\":{\"k\":{\"l\":{\"m\":{\"n\":{\"o\":{\"p\":\"boom\"}}}}}}}}}}}}}}}}",
                "{\"a\":{\"b\":{\"c\":{\"d\":{\"e\":{\"f\":{\"g\":{\"h\":{\"i\":{\"j\":{\"k\":{\"l\":{\"m\":{\"n\":{\"o\":{\"p\":\"boom\"}}}}}}}}}}}}}}}}");

        testFail("{\"a\":{\"b\":{\"c\":{\"d\":{\"e\":{\"f\":{\"g\":{\"h\":{\"i\":{\"j\":{\"k\":{\"l\":{\"m\":{\"n\":{\"o\":{\"p\":\"boom\"}}}}}}}}}}}}}}}}",
                "{\"a\":{\"b\":{\"c\":{\"d\":{\"e\":{\"f\":{\"g\":{\"h\":{\"i\":{\"j\":{\"k\":{\"l\":{\"m\":{\"n\":{\"o\":{\"p\":\"boom1\", \"k\":\"v\"}}}}}}}}}}}}}}}}",
                "Expected value at '/a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p' to be 'boom', but is 'boom1'");
    }

    @Test
    public void testArray()
            throws Exception
    {
        testPass("[1,2,3]", "[1,2,3]");
        testPass("[1,2,3]", "[1,3,2]");
        testPass("[1,1,1]", "[1,1,1]");
        testPass("[1,2]", "[1,3,2]");

        testFail("[1,2,3,4]", "[1,3,2]",
                "Expected 4 values but got 3 at '' array");
        testFail("[1,2,3]", "[4,5,6]",
                "Could not find match for element '1' at '[0]'. Possible mismatch: Expected value at '[0]' to be '1', but is '4'");
    }

    @Test
    public void testSimpleArray()
            throws Exception
    {
        testPass("{\"id\":1, \"pets\":[\"dog\",\"cat\",\"fish\"]}",
                "{\"id\":1, \"pets\":[\"dog\",\"cat\",\"fish\"]}");

        testFail("{\"id\":1, \"pets\": [\"dog\",\"cat\",\"fish\"]}",
                "{\"id\":1, \"pets\":[\"dog\",\"cat\",\"bird\"]}",
                "Could not find match for element 'fish' at '/pets[2]'. Possible mismatch: Expected value at '/pets[2]' to be 'fish', but is 'bird'");
    }

    @Test
    public void testNullProperty()
            throws Exception
    {
        testFail("{\"id\":1, \"name\":\"Sherlock\"}", "{\"id\":1, \"name\":null}",
                "Expected value at '/name' to be 'Sherlock', but is 'null'");

        testFail("{\"id\":1, \"name\":null}", "{\"id\":1, \"name\":\"Sherlock\"}",
                "Expected value at '/name' to be 'null', but is 'Sherlock'");
    }

    @Test
    public void testSimpleMixedArray()
            throws Exception
    {
        testPass("{\"stuff\": [321, \"abc\"]}", "{\"stuff\":[\"abc\", 321]}");

        testFail("{\"stuff\": [321, \"abc\"]}", "{\"stuff\":[\"abc\", 789]}",
                "Could not find match for element '321' at '/stuff[0]'. Possible mismatch: Expected value at '/stuff[0]' to be '321', but is 'abc'");
    }

    @Test
    public void testComplexMixedArray()
            throws Exception
    {
        testPass("{\"stuff\": [{\"pet\":\"cat\"}, {\"car\":\"Ford\"}]}",
                "{\"stuff\":[{\"pet\":\"cat\"}, {\"car\":\"Ford\"}]}");
    }

    @Test
    public void testArrayOfArrays()
            throws Exception
    {
        testPass("{\"id\":1, \"stuff\": [[1,2],[2,3],[],[3,4]]}", "{\"id\":1, \"stuff\": [[1,2],[2,3],[],[3,4]]}");
        testFail("{\"id\":1, \"stuff\": [[1,2],[2,3],[3,4],[42]]}", "{\"id\":1, \"stuff\": [[1,2],[2,3],[],[3,4]]}",
                "Could not find match for element '<array[1]>' at '/stuff[3]'. Possible mismatch: Could not find match for element '42' at '/stuff[3][0]'. Possible mismatch: Expected value at '/stuff[3][0]' to be '42', but is '3'");
    }

    @Test
    public void testIncorrectTypes()
            throws Exception
    {
        testFail("{\"id\":1, \"name\":\"Sherlock\"}", "{\"id\":1, \"name\":[]}",
                "Expected value at '/name' to be 'Sherlock', but is '<array[0]>'");

        testFail("{\"id\":1, \"name\":[]}", "{\"id\":1, \"name\":\"Sherlock\"}",
                "Expected value at '/name' to be '<array[0]>', but is 'Sherlock'");
    }

    @Test
    public void testExpectedArrayButActualObject()
            throws Exception
    {
        testFail("[1]", "{\"id\":1}", "Expected value at '' to be '<array[1]>', but is '<object>'");
    }

    @Test
    public void testExpectedObjectButActualArray()
            throws Exception
    {
        testFail("{\"id\":1}", "[1]", "Expected value at '' to be '<object>', but is '<array[1]>'");
    }

    @Test
    public void testNullEquality()
            throws Exception
    {
        testPass("{\"id\":1, \"name\": null}", "{\"id\":1, \"name\": null}");
    }

    @Test
    public void test001()
            throws Exception
    {
        testPassFiles("001");
    }

    @Test
    public void test002()
            throws Exception
    {
        testFailFiles("002", true, "Expected 4 values but got 6 at '/Statement'");
    }

    @Test
    public void test003() throws Exception {
        testFailFiles("003", true, "Field mismatch at '/metadata': missing fields: annotations, creationTimestamp, generation, labels, resourceVersion, uid;");
    }

    private void testPassFiles(String name)
            throws Exception
    {
        JsonNode expected = objectMapper.readValue(resourcesAsFile(name + "-expected.json").toFile(), JsonNode.class);
        JsonNode actual = objectMapper.readValue(resourcesAsFile(name + "-actual.json").toFile(), JsonNode.class);
        JsonCompareResult result = comparator.compare(expected, actual);
        assertTrue(result.message(), result.success());
    }

    private void testFailFiles(String name, boolean strictMatch, String expectedError)
            throws Exception
    {
        JsonNode expected = objectMapper.readValue(resourcesAsFile(name + "-expected.json").toFile(), JsonNode.class);
        JsonNode actual = objectMapper.readValue(resourcesAsFile(name + "-actual.json").toFile(), JsonNode.class);
        JsonCompareResult result = new JsonComparator(strictMatch).compare(expected, actual);

        String message = expected + " != " + actual;
        assertTrue(message, result.failed());
        assertEquals(expectedError, result.message());
    }

    private void testPass(String expected, String actual)
            throws Exception
    {
        String message = expected + " == " + actual;
        JsonCompareResult result = comparator.compare(objectMapper.readValue(expected, JsonNode.class), objectMapper.readValue(actual, JsonNode.class));
        assertTrue(message + "\n  " + result.message(), result.success());
    }

    private void testFail(String expected, String actual, String error)
            throws Exception
    {
        String message = expected + " != " + actual;
        JsonCompareResult result = comparator.compare(objectMapper.readValue(expected, JsonNode.class), objectMapper.readValue(actual, JsonNode.class));
        System.out.println(">>>" + result.message());
        assertTrue(message, result.failed());
        assertEquals(error, result.message());
    }
}
