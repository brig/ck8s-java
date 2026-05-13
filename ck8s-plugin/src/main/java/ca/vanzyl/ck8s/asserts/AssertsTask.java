package ca.vanzyl.ck8s.asserts;

import ca.vanzyl.ck8s.InterpolateTask;
import ca.vanzyl.ck8s.asserts.json.JsonComparator;
import ca.vanzyl.ck8s.asserts.json.JsonCompareResult;
import ca.vanzyl.ck8s.secrets.BootstrapSecretsTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;

@Named("asserts")
@DryRunReady
@SuppressWarnings("unused")
public class AssertsTask
        implements Task {

    private final static Logger log = LoggerFactory.getLogger(AssertsTask.class);

    private final ObjectMapper jsonObjectMapper = new ObjectMapper();
    private final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());

    private final Context context;
    private final BootstrapSecretsTask secrets;
    private final InterpolateTask interpolateTask;

    @Inject
    public AssertsTask(Context context, BootstrapSecretsTask secrest, InterpolateTask interpolateTask) {
        this.context = context;
        this.secrets = secrest;
        this.interpolateTask = interpolateTask;
    }

    public void dryRunMode() {
        if (!context.processConfiguration().dryRun()) {
            throw new UserDefinedException("Expected dry run mode to be enabled in process configuration");
        }
    }

    public static Object assertNotNull(Object value) {
        return assertNotNull("value is null", value);
    }

    public static Object assertNotNull(String message, Object value) {
        assertTrue(message, value != null);
        return value;
    }

    public static String assertNotEmpty(String message, String value) {
        if (value == null || value.isBlank()) {
            throw new UserDefinedException(message);
        }

        return value;
    }

    public static Object assertNotEmpty(String message, Object value) {
        if (value == null) {
            throw new UserDefinedException(message);
        }

        if (value instanceof String s) {
            return assertNotEmpty(message, s);
        }

        return value;
    }

    public static void assertNoNullValues(Map<String, Object> m) {
        m.forEach((key, value) -> assertNotEmpty("Empty value found for argument: " + key, value));
    }

    private static boolean isSpecialNumber(Number x) {
        boolean specialDouble = x instanceof Double
                && (Double.isNaN((Double) x) || Double.isInfinite((Double) x));
        boolean specialFloat = x instanceof Float
                && (Float.isNaN((Float) x) || Float.isInfinite((Float) x));
        return specialDouble || specialFloat;
    }

    private static BigDecimal toBigDecimal(Number number) {
        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        } else if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger) number);
        } else if (number instanceof Byte || number instanceof Short
                || number instanceof Integer || number instanceof Long) {
            return BigDecimal.valueOf(number.longValue());
        } else if (number instanceof Float || number instanceof Double) {
            return BigDecimal.valueOf(number.doubleValue());
        }

        try {
            return new BigDecimal(number.toString());
        } catch (NumberFormatException e) {
            throw new RuntimeException("The given number (\"" + number + "\" of class " + number.getClass().getName() + ") does not have a parsable string representation", e);
        }
    }

    public void assertEquals(Object expected, Object actual) {
        if (expected == null && actual == null) {
            return;
        }

        if (expected == null) {
            throw new UserDefinedException("Expected value to be 'null' but is '" + actual + "'");
        } else if (actual == null) {
            throw new UserDefinedException("Expected value to be '" + expected + "' but is 'null'");
        }

        if (expected instanceof Number && actual instanceof Number) {
            if (numbersEquals((Number) expected, (Number) actual)) {
                return;
            }
        } else if (expected.equals(actual)) {
            return;
        }

        String msg = String.format("Expected value to be '%s' (class: %s) but is '%s' (class: %s)",
                expected, expected.getClass().getName(),
                actual, actual.getClass().getName());

        throw new UserDefinedException(msg);
    }

    public void assertEndsWith(String str, String suffix) {
        if (str == null) {
            throw new UserDefinedException("Expected value to ends with'" + suffix + "' but value is null");
        }

        if (str.endsWith(suffix)) {
            return;
        }

        throw new UserDefinedException("Expected value to ends with'" + suffix + "' but is '" + str + "'");
    }

    public static void assertFalse(boolean condition) {
        if (condition) {
            throw new UserDefinedException("Expected value to be false but is true");
        }
    }

    public static void assertFalse(String message, boolean condition) {
        if (condition) {
            throw new UserDefinedException(message);
        }
    }

    public static void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new UserDefinedException(message);
        }
    }

    public static void assertTrue(boolean condition) {
        if (!condition) {
            throw new UserDefinedException("Expected value to be true but is false");
        }
    }

    private boolean numbersEquals(Number expected, Number actual) {
        if (isSpecialNumber(expected) || isSpecialNumber(actual)) {
            return Double.compare(expected.doubleValue(), actual.doubleValue()) == 0;
        } else {
            return toBigDecimal(expected).compareTo(toBigDecimal(actual)) == 0;
        }
    }

    public void secret(String name) {
        String value = secrets.get(name);
        if (value == null || value.trim().isEmpty()) {
            throw new UserDefinedException("Secret '" + name + "' not found");
        }
    }

    public Object hasVariable(String name) {
        boolean present = context.eval(String.format("${hasVariable('%s')}", name), Boolean.class);
        if (!present) {
            throw new UserDefinedException("Variable '" + name + "' not found");
        }

        Object value = context.eval("${" + name + "}", Object.class);
        if (value == null) {
            throw new UserDefinedException("Variable '" + name + "' is null value");
        }

        if (value instanceof String) {
            if (((String) value).trim().isEmpty()) {
                throw new UserDefinedException("Variable '" + name + "' is empty");
            }
        }
        return value;
    }

    public void assertYaml(String expectedPath, Object actualObject) {
        assertYaml(expectedPath, actualObject, true);
    }

    public void assertYaml(String expectedPath, Object actualObject, boolean interpolateExpected) {
        assertDocument(yamlObjectMapper, expectedPath, actualObject, interpolateExpected, false);
    }

    public void assertYamlEquals(String expectedPath, Object actualObject) {
        assertYamlEquals(expectedPath, actualObject, true);
    }

    public void assertYamlEquals(String expectedPath, Object actualObject, boolean interpolateExpected) {
        assertDocument(yamlObjectMapper, expectedPath, actualObject, interpolateExpected, true);
    }

    public void assertJson(String expectedPath, Object actualObject) {
        assertJson(expectedPath, actualObject, true);
    }

    public void assertJson(String expectedPath, Object actualObject, boolean interpolateExpected) {
        assertDocument(jsonObjectMapper, expectedPath, actualObject, interpolateExpected, false);
    }

    public void assertJsonEquals(String expectedPath, Object actualObject) {
        assertJsonEquals(expectedPath, actualObject, true);
    }

    public void assertJsonEquals(String expectedPath, Object actualObject, boolean interpolateExpected) {
        assertDocument(jsonObjectMapper, expectedPath, actualObject, interpolateExpected, true);
    }

    private Path resolvePath(String rootVariableName, String expectedPath) {
        Path path = Path.of(expectedPath);
        if (path.isAbsolute() || Files.exists(path)) {
            return path;
        }

        String root = context.variables().getString(rootVariableName);
        if (root == null) {
            return null;
        }

        Path result = Path.of(root).resolve(path);
        if (Files.notExists(result)) {
            return null;
        }

        return result;
    }

    private void assertDocument(ObjectMapper objectMapper, String expectedPath, Object actualObject, boolean interpolateExpected, boolean strictMatch)
    {
        Path expectedTemplatePath = resolvePath("componentsTests", expectedPath);
        if (expectedTemplatePath == null) {
            expectedTemplatePath = resolvePath("components", expectedPath);
        }
        if (expectedTemplatePath == null) {
            throw new UserDefinedException("Can't find test template '" + expectedPath + "'");
        }

        Path p;
        try {
            p = context.fileService().createTempFile(expectedTemplatePath.getFileName().toString(), "template.yaml");
            Files.copy(expectedTemplatePath, p, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (Exception e) {
            throw new UserDefinedException("Error prepare template '" + expectedPath + "' error: " + e.getMessage());
        }

        try (TemporaryFile f = new TemporaryFile(p)) {
            if (interpolateExpected) {
                try {
                    interpolateTask.execute(new MapBackedVariables(Collections.singletonMap("file", p.toString())));
                } catch (Exception e) {
                    throw new UserDefinedException("Interpolate template '" + expectedPath + "' error: " + e.getMessage());
                }
            }

            JsonNode expected;
            try {
                expected = objectMapper.readValue(p.toFile(), JsonNode.class);
            }
            catch (Exception e) {
                throw new UserDefinedException("Can't read test template '" + expectedPath + "': " + e.getMessage());
            }

            try {
                if (actualObject instanceof String) {
                    actualObject = objectMapper.readValue((String) actualObject, Object.class);
                }
            }
            catch (Exception e) {
                throw new UserDefinedException("Can't convert actual value from string to json: " + e.getMessage());
            }

            JsonNode actual = objectMapper.convertValue(actualObject, JsonNode.class);

            JsonCompareResult result = new JsonComparator(new ValueComparatorWithEl(context.variables()), strictMatch)
                    .compare(expected, actual);

            log.info("Current: \n{}", objectToString(objectMapper, actualObject));
            log.info("Expected: \n{}", objectToString(objectMapper, expected));

            if (result.failed()) {
                throw new UserDefinedException(result.message());
            }
        }
    }

    public void assertObjectsAsJson(Object expectedObject, Object actualObject) {
        JsonNode expected = jsonObjectMapper.convertValue(expectedObject, JsonNode.class);
        JsonNode actual = jsonObjectMapper.convertValue(actualObject, JsonNode.class);

        JsonCompareResult result = new JsonComparator(new ValueComparatorWithEl(context.variables()), true)
                .compare(expected, actual);

        if (result.failed()) {
            throw new UserDefinedException(result.message());
        }
    }

    private static String objectToString(ObjectMapper objectMapper, Object actualObject) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actualObject);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    static class TemporaryFile
            implements AutoCloseable
    {

        private final Path path;

        public TemporaryFile(Path path)
        {
            this.path = path;
        }

        public Path path()
        {
            return path;
        }

        @Override
        public void close()
        {
            if (path == null) {
                return;
            }

            try {
                Files.deleteIfExists(path);
            }
            catch (IOException e) {
                log.warn("cleanup ['{}'] -> error: {}", path, e.getMessage());
            }
        }
    }
}
