package ca.vanzyl.ck8s.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MapUtils
{

    private MapUtils()
    {
    }

    public static <K, V> Map<K, V> assertNoNullValues(Map<K, V> m) {
        for (var entry : m.entrySet()) {
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("null value for key: '" + entry.getKey() + "'");
            }
        }
        return m;
    }

    public static boolean getBoolean(Map<String, Object> m, String path, boolean defaultValue)
    {
        Boolean result = get(m, path, defaultValue, Boolean.class);
        if (result != null) {
            return result;
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getList(Map<String, Object> m, String path)
    {
        return get(m, path, Collections.emptyList(), List.class);
    }

    @SuppressWarnings("unchecked")
    public static <T> Map<String, T> getMap(Map<String, Object> m, String path, Map<String, Object> defaultValue)
    {
        return get(m, path, defaultValue, Map.class);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> assertMap(Map<String, Object> m, String path)
    {
        return assertValue(m, path, Map.class);
    }

    public static String assertString(Map<String, Object> m, String path)
    {
        return assertValue(m, path, String.class);
    }

    public static String getString(Map<String, Object> m, String path)
    {
        return getString(m, path, null);
    }

    public static int getInt(Map<String, Object> m, String path, int defaultValue)
    {
        return get(m, path, defaultValue, Integer.class);
    }

    public static Number getNumber(Map<String, Object> m, String path, Number defaultValue) {
        return get(m, path, defaultValue, Number.class);
    }

    public static String getString(Map<String, Object> m, String path, String defaultValue)
    {
        return get(m, path, defaultValue, String.class);
    }

    public static <T> T get(Map<String, Object> m, String path, T defaultValue, Class<T> type)
    {
        Object value = getObject(m, path.split("\\."));
        if (value == null) {
            return defaultValue;
        }
        else if (type.isInstance(value)) {
            return type.cast(value);
        }
        else {
            throw new IllegalArgumentException("Invalid value type at '" + path + "', expected: " + type + ", got: " + value.getClass());
        }
    }

    public static <T> T assertValue(Map<String, Object> m, String path, Class<T> type)
    {
        T result = get(m, path, null, type);
        if (result != null) {
            return result;
        }
        else {
            throw new IllegalArgumentException("Mandatory value at '" + path + "' is required");
        }
    }

    public static Object getObject(Map<String, Object> m, String... path)
    {
        int depth = path != null ? path.length : 0;
        return getObject(m, depth, path);
    }

    @SuppressWarnings("unchecked")
    public static Object getObject(Map<String, Object> m, int depth, String... path)
    {
        if (m == null) {
            return null;
        }

        if (depth == 0) {
            return m;
        }

        for (int i = 0; i < depth - 1; i++) {
            Object v = m.get(path[i]);
            if (v == null) {
                return null;
            }

            if (!(v instanceof Map)) {
                throw new IllegalArgumentException("Invalid data type, expected JSON object, got: " + v.getClass());
            }

            m = (Map<String, Object>) v;
        }

        return m.get(path[depth - 1]);
    }

    public static Map<String, Object> merge(Map<String, Object> a, Map<String, Object> b)
    {
        return MergeUtils.merge(a, b);
    }

    @SuppressWarnings("unchecked")
    public static void set(Map<String, Object> m, Object b, String fullPath)
    {
        String[] path = fullPath.split("\\.");

        ensurePath(m, path.length - 1, path);

        Object holder = getObject(m, path.length - 1, path);
        if (!(holder instanceof Map)) {
            throw new IllegalArgumentException("Value should be contained in a JSON object: " + String.join("/", path));
        }

        Map<String, Object> mm = (Map<String, Object>) holder;
        mm.put(path[path.length - 1], b);
    }

    private static void ensurePath(Map<String, Object> m, int depth, String[] path)
    {
        for (int i = 0; i < depth; i++) {
            Map<String, Object> mm = MapUtils.getMap(m, path[i], null);
            if (mm == null) {
                m.put(path[i], new LinkedHashMap<>());
            }
            m = MapUtils.getMap(m, path[i], null);
        }
    }
}
