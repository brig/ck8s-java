package ca.vanzyl.ck8s.utils;

import ca.vanzyl.ck8s.common.Mapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import org.immutables.value.Value;

import javax.inject.Named;
import java.nio.file.Paths;
import java.util.*;

@Named("triggersUtils")
public class TriggerUtilsTask implements Task {

    public boolean matchFiles(Map<String, List<Map<String, Object>>> triggerDefinitionFiles, Map<String, List<String>> eventFiles) {
        if (triggerDefinitionFiles == null || triggerDefinitionFiles.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, List<Map<String, Object>>> t : triggerDefinitionFiles.entrySet()) {
            List<PathMapping> triggerMapping = Mapper.yaml().convertValue(t.getValue(), new TypeReference<>() {});;
            List<String> files = eventFiles.get(t.getKey());

            for (String file : files) {
                if (triggerMapping.stream().anyMatch(m -> isMatch(file, m))) {
                    return true;
                }
            }
        }

        return false;
    }

    public Set<String> matchFlows(List<String> files, String mappingFileName) {
        if (files == null || files.isEmpty()) {
            return Collections.emptySet();
        }

        Map<String, List<PathMapping>> mappings = Mapper.yaml().read(Paths.get(mappingFileName), new TypeReference<>() {});

        Set<String> result = new HashSet<>();
        for (Map.Entry<String, List<PathMapping>> e : mappings.entrySet()) {
            String flowName = e.getKey();
            for (String file : files) {
                if (e.getValue().stream().anyMatch(m -> isMatch(file, m))) {
                    result.add(flowName);
                }
            }
        }

        return result;
    }

    private static boolean isMatch(String file, PathMapping mapping) {
        String root = addSlashToEnd(mapping.root());

        if (!isMatch(file, root)) {
            return false;
        }

        for (String pattern : mapping.exclude()) {
            if (isMatch(file, root + pattern)) {
                return false;
            }
        }

        for (String pattern : mapping.include()) {
            if (isMatch(file, pattern)) {
                return true;
            }
        }

        return mapping.include().isEmpty();
    }

    private static boolean isMatch(String file, String pattern) {
        return file.startsWith(pattern) || file.matches(pattern);
    }

    private static String addSlashToEnd(String input) {
        if (input.endsWith("/")) {
            return input;
        }

        return input + "/";
    }

    @Value.Immutable
    @Value.Style(jdkOnly = true)
    @JsonSerialize(as = ImmutablePathMapping.class)
    @JsonDeserialize(as = ImmutablePathMapping.class)
    interface PathMapping {

        String root();

        @Value.Default
        default List<String> include() {
            return Collections.emptyList();
        }

        @Value.Default
        default List<String> exclude() {
            return Collections.emptyList();
        }
    }
}
