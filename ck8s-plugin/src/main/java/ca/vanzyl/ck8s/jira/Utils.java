package ca.vanzyl.ck8s.jira;

import java.util.Map;

public final class Utils {

    public static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private Utils() {
    }
}
