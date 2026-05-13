package ca.vanzyl.ck8s.helm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class JsonMapper {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, Object> asMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }

        try {
            return mapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON: " + json, e);
        }
    }
}
