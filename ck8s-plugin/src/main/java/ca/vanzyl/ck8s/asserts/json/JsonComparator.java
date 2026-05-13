package ca.vanzyl.ck8s.asserts.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

import java.util.*;

public class JsonComparator {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ValueComparator valueComparator;

    private final boolean strictMatch;

    public JsonComparator() {
        this(false);
    }

    public JsonComparator(boolean strictMatch) {
        this((expected, actual) -> expected.equals(String.valueOf(actual)), strictMatch);
    }

    public JsonComparator(ValueComparator valueComparator, boolean strictMatch) {
        this.valueComparator = valueComparator;
        this.strictMatch = strictMatch;
    }

    private static String valueMismatchError(JsonPath path, JsonNode expected, JsonNode actual) {
        return String.format("Expected value at '%s' to be '%s', but is '%s'", path,
                toString(expected), toString(actual));
    }

    private static String missingError(JsonPath path, JsonNode expectedValue) {
        return String.format("Missing value '%s' at '%s'", toString(expectedValue), path);
    }

    private String fieldMismatchError(JsonPath path, ObjectNode expected, ObjectNode actual) {
        List<String> missingFields = new ArrayList<>();
        List<String> unexpectedFields = new ArrayList<>();

        Iterator<String> expectedFieldNames = expected.fieldNames();
        while (expectedFieldNames.hasNext()) {
            String key = expectedFieldNames.next();
            if (!actual.has(key)) {
                missingFields.add(key);
            }
        }

        Iterator<String> actualFieldNames = actual.fieldNames();
        while (actualFieldNames.hasNext()) {
            String key = actualFieldNames.next();
            if (!expected.has(key)) {
                unexpectedFields.add(key);
            }
        }

        StringBuilder sb = new StringBuilder("Field mismatch at '").append(path).append("':");

        if (!missingFields.isEmpty()) {
            sb.append(" missing fields: ").append(String.join(", ", missingFields)).append(";");
        }
        if (!unexpectedFields.isEmpty()) {
            sb.append(" unexpected fields: ").append(String.join(", ", unexpectedFields)).append(";");
        }

        return sb.toString();
    }

    private String unexpectFieldError(JsonPath path, ArrayNode expected, ArrayNode actual) {
        return String.format("Expected %d values but got %d at '%s'", expected.size(), actual.size(), path);
    }

    private static String toString(JsonNode node) {
        if (node == null) {
            return "null";
        }

        if (node.isArray()) {
            return "<array[" + node.size() + "]>";
        } else if (node.isObject()) {
            return "<object>";
        }
        return node.asText();
    }

    public JsonCompareResult compare(JsonNode expected, JsonNode actual) {
        JsonPath path = new JsonPath();
        return compare(path, expected, actual);
    }

    public JsonCompareResult compare(JsonPath path, JsonNode expected, JsonNode actual) {

        if ((expected instanceof ObjectNode) && (actual instanceof ObjectNode)) {
            return compare(path, (ObjectNode) expected, (ObjectNode) actual);
        } else if ((expected instanceof ArrayNode) && (actual instanceof ArrayNode)) {
            return compare(path, (ArrayNode) expected, (ArrayNode) actual);
        } else if (expected instanceof ValueNode && actual instanceof ValueNode) {
            return compareValues(path, expected, actual);
        }

        return JsonCompareResult.fail(valueMismatchError(path, expected, actual));
    }

    private JsonCompareResult compare(JsonPath path, ObjectNode expected, ObjectNode actual) {
        if (strictMatch) {
            if (expected.size() != actual.size()) {
                return JsonCompareResult.fail(fieldMismatchError(path, expected, actual));
            }
        }

        Iterator<String> fieldNames = expected.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode expectedValue = expected.get(fieldName);
            if (actual.has(fieldName)) {
                JsonNode actualValue = actual.get(fieldName);
                JsonCompareResult result = compareValues(path.field(fieldName), expectedValue, actualValue);
                if (result.failed()) {
                    return result;
                }
            } else {
                return JsonCompareResult.fail(missingError(path.field(fieldName), expectedValue));
            }
        }
        return JsonCompareResult.ok();
    }

    private JsonCompareResult compareValues(JsonPath path, JsonNode expectedValue, JsonNode actualValue) {
        if (expectedValue == actualValue) {
            return JsonCompareResult.ok();
        }

        if ((expectedValue == null && actualValue != null) || (expectedValue != null && actualValue == null)) {
            return JsonCompareResult.fail(valueMismatchError(path, expectedValue, actualValue));
        }

        if (expectedValue.getClass().isAssignableFrom(actualValue.getClass())) {
            if (expectedValue instanceof ArrayNode) {
                return compare(path, (ArrayNode) expectedValue, (ArrayNode) actualValue);
            } else if (expectedValue instanceof ObjectNode) {
                return compare(path, (ObjectNode) expectedValue, (ObjectNode) actualValue);
            }
        }

        if (!compareValues(expectedValue, actualValue)) {
            return JsonCompareResult.fail(valueMismatchError(path, expectedValue, actualValue));
        }

        return JsonCompareResult.ok();
    }

    private JsonCompareResult compare(JsonPath path, ArrayNode expected, ArrayNode actual) {
        if (strictMatch) {
            if (expected.size() != actual.size()) {
                return JsonCompareResult.fail(unexpectFieldError(path, expected, actual));
            }
        }

        if (expected.size() > actual.size()) {
            String error = String.format("Expected %d values but got %d at '%s' array", expected.size(), actual.size(), path);
            return JsonCompareResult.fail(error);
        } else if (expected.size() == 0) {
            return JsonCompareResult.ok();
        }
        return compareJsonArray(path, expected, actual);
    }

    protected JsonCompareResult compareJsonArray(JsonPath path, ArrayNode expected, ArrayNode actual) {
        Set<Integer> matched = new HashSet<>();
        for (int i = 0; i < expected.size(); ++i) {
            JsonNode expectedElement = expected.get(i);
            boolean matchFound = false;
            for (int j = 0; j < actual.size(); ++j) {
                JsonNode actualElement = actual.get(j);
                if (expectedElement == actualElement) {
                    matchFound = true;
                    break;
                }

                if ((expectedElement == null && actualElement != null) || (expectedElement != null && actualElement == null)) {
                    continue;
                }

                if (matched.contains(j) || !actualElement.getClass().equals(expectedElement.getClass())) {
                    continue;
                }

                if (expectedElement instanceof ObjectNode) {
                    if (compare(path.index(i), (ObjectNode) expectedElement, (ObjectNode) actualElement).success()) {
                        matched.add(j);
                        matchFound = true;
                        break;
                    }
                } else if (expectedElement instanceof ArrayNode) {
                    if (compare(path.index(i), (ArrayNode) expectedElement, (ArrayNode) actualElement).success()) {
                        matched.add(j);
                        matchFound = true;
                        break;
                    }
                } else if (compareValues(expectedElement, actualElement)) {
                    matched.add(j);
                    matchFound = true;
                    break;
                }
            }

            if (!matchFound) {
                JsonCompareResult r = tryFindMismatch(path.index(i), expected.get(i), actual.get(i));

                String error = String.format("Could not find match for element '%s' at '%s'. Possible mismatch: %s",
                        toString(expectedElement), path.index(i), r.message());

                return JsonCompareResult.fail(error);
            }
        }

        return JsonCompareResult.ok();
    }

    private boolean compareValues(JsonNode expected, JsonNode actual) {
        return valueComparator.equals(expected.asText(), objectMapper.convertValue(actual, Object.class));
    }

    private JsonCompareResult tryFindMismatch(JsonPath path, JsonNode expected, JsonNode actual) {
        return compare(path, expected, actual);
    }

    public interface ValueComparator {

        boolean equals(String expected, Object actual);
    }
}
