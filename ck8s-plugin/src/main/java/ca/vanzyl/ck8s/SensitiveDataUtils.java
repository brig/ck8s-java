package ca.vanzyl.ck8s;

import com.walmartlabs.concord.runtime.v2.sdk.SensitiveDataHolder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;


public final class SensitiveDataUtils {

    static final int MAX_DEPTH = 128;

    public static <T> T hideSensitiveData(SensitiveDataHolder holder, T value) {
        return hideSensitiveData(holder, value, Set.of());
    }

    @SuppressWarnings("unchecked")
    public static <T> T hideSensitiveData(SensitiveDataHolder holder, T value, Set<String> exclusions) {
        var sensitiveData = holder.get();
        if (sensitiveData.isEmpty()) {
            return value;
        }
        var effectiveSensitiveData = new HashSet<>(sensitiveData);
        effectiveSensitiveData.removeAll(exclusions);
        return (T) hideSensitiveData(effectiveSensitiveData, value, 0);
    }

    private static Object hideSensitiveData(Set<String> sensitiveData, Object value, int depth) {
        if (depth > MAX_DEPTH) {
            return "...to deep";
        }

        if (value instanceof String v) {
            for (String s : sensitiveData) {
                v = v.replace(s, "_*****");
            }
            return v;
        }

        if (value instanceof Map<?, ?> m) {
            return m.entrySet()
                    .stream()
                    .collect(toMap(
                            Map.Entry::getKey,
                            e -> hideSensitiveData(sensitiveData, e.getValue(), depth + 1),
                            (a, b) -> b,
                            LinkedHashMap::new));
        }

        if (value instanceof List<?> l) {
            return l.stream()
                    .map(e -> hideSensitiveData(sensitiveData, e, depth + 1))
                    .collect(Collectors.toList());
        }

        return value;
    }

    private SensitiveDataUtils() {
    }
}
