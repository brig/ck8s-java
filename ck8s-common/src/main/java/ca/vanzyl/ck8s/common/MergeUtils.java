package ca.vanzyl.ck8s.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MergeUtils
{

    private MergeUtils()
    {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> merge(Map<String, Object> a, Map<String, Object> b)
    {
        Map<String, Object> result = new LinkedHashMap<>(a != null ? a : Collections.emptyMap());

        for (String k : b.keySet()) {
            Object av = result.get(k);
            Object bv = b.get(k);

            Object o = bv;
            if (av instanceof Map && bv instanceof Map) {
                o = merge((Map<String, Object>) av, (Map<String, Object>) bv);
            }

            // preserve the order of the keys
            if (result.containsKey(k)) {
                result.replace(k, o);
            }
            else {
                result.put(k, o);
            }
        }
        return result;
    }
}
