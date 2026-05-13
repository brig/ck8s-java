package ca.vanzyl.ck8s.asserts.json;

import ca.vanzyl.ck8s.common.Mapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.zjsonpatch.DiffFlags;
import io.fabric8.zjsonpatch.JsonDiff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

public class JsonComparatorV2 {

    private final static Logger log = LoggerFactory.getLogger(JsonComparatorV2.class);

    private static final TypeReference<JsonNode> JSON_NODE_TYPE = new TypeReference<>()
    {
    };

    public JsonCompareResult compare(Map<String, Object> expected, Map<String, Object> actual) {
        return compare(expected, actual, m -> Mapper.json().convertValue(m, JSON_NODE_TYPE));
    }

    public JsonCompareResult compare(String expectedJson, String actualJson) {
        return compare(expectedJson, actualJson, json -> {
            try {
                return Mapper.json().read(json, JSON_NODE_TYPE);
            } catch (IOException e) {
                log.error("Failed to parse JSON from string:\n{}\n{}", json, e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    private <T> JsonCompareResult compare(T expected, T actual, Function<T, JsonNode> converter) {
        if (expected == null && actual == null) {
            return JsonCompareResult.ok();
        }

        if (expected == null) {
            return JsonCompareResult.fail("expected is null");
        }

        if (actual == null) {
            return JsonCompareResult.fail("actual is null");
        }

        JsonNode expectedNode = converter.apply(expected);
        JsonNode actualNode = converter.apply(actual);

        var flags = DiffFlags.dontNormalizeOpIntoMoveAndCopy().clone();
        var result = JsonDiff.asJson(actualNode, expectedNode, flags);
        if (result.isEmpty()) {
            return JsonCompareResult.ok();
        }

        var message = Mapper.json().writeAsString(result);
        return new JsonCompareResult(false, message);
    }
}
