package ca.vanzyl.ck8s.utils;

import ca.vanzyl.ck8s.asserts.json.JsonComparator;
import ca.vanzyl.ck8s.asserts.json.JsonCompareResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.walmartlabs.concord.runtime.v2.sdk.Task;

import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Named("diff")
public class DiffTask implements Task {

    private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());

    public Map<String, Object> computeForYaml(String onePath, String twoPath) throws IOException {
        var oneEmpty = K8sFileUtils.isEmpty(onePath);
        var twoEmpty = K8sFileUtils.isEmpty(twoPath);
        if (oneEmpty && twoEmpty) {
            return Map.of("noDiff", true);
        } else if (oneEmpty) {
            return Map.of("noDiff", false, "message", "new file");
        } else if (twoEmpty) {
            return Map.of("noDiff", false, "message", "file removed");
        }

        var oneNode = yamlObjectMapper.readValue(Path.of(onePath).toFile(), JsonNode.class);
        var twoNode = yamlObjectMapper.readValue(Path.of(twoPath).toFile(), JsonNode.class);

        JsonCompareResult result = new JsonComparator(true)
                .compare(oneNode, twoNode);

        if (result.message() != null) {
            return Map.of("noDiff", result.success(), "message", result.message());
        } else {
            return Map.of("noDiff", result.success());
        }
    }

    public List<Collection<String>> compute(Object ... items) {
        return compute(Arrays.asList(items));
    }

    public List<Collection<String>> compute(List<Object> items) {
        List<Collection<String>> normalizedInput = normalizeInput(items);

        List<Collection<String>> result = new ArrayList<>();
        for (Collection<String> list : normalizedInput) {
            result.add(new HashSet<>(list));
        }

        for (int i = 0; i < normalizedInput.size(); i++) {
            Set<String> uniqueToCurrent = new HashSet<>(normalizedInput.get(i));
            for (int j = 0; j < normalizedInput.size(); j++) {
                if (i != j) {
                    uniqueToCurrent.removeAll(normalizedInput.get(j));
                }
            }
            result.set(i, uniqueToCurrent);
        }

        if (result.stream().flatMap(Collection::stream).toList().isEmpty()) {
            return List.of();
        }

        return result;
    }

    public static List<Collection<String>> normalizeInput(List<Object> items) {
        return items.stream()
                .map(DiffTask::normalizeInput)
                .toList();
    }

    private static Collection<String> normalizeInput(Object a) {
        if (a instanceof Collection<?>) {
            return ((Collection<?>) a).stream()
                    .map(Object::toString)
                    .map(String::trim)
                    .toList();
        } else if (a instanceof String) {
            return ((String) a).lines()
                    .map(String::trim)
                    .toList();
        } else if (a instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) a;
            return m.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.toList());
        } else if (a == null) {
            return List.of();
        }

        throw new RuntimeException("Unsupported input type: " + a.getClass());
    }
}
