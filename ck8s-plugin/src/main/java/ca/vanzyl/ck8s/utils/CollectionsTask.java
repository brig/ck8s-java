package ca.vanzyl.ck8s.utils;

import com.google.common.collect.Lists;
import com.walmartlabs.concord.runtime.v2.sdk.DryRunReady;
import com.walmartlabs.concord.runtime.v2.sdk.Task;

import javax.inject.Named;
import java.util.*;
import java.util.stream.Collectors;

// TODO: remove after concord 2.24.x released
@Named("ck8sCollections")
@DryRunReady
public class CollectionsTask
        implements Task
{

    public List<String> keySetAsListFrom(Map<String, Object> map)
    {
        return new ArrayList<>(map.keySet());
    }

    @SuppressWarnings("unchecked")
    public static List<String> toList(Object o) {
        if (o == null) {
            return null;
        }

        if (o instanceof List<?>) {
            return (List<String>) o;
        }

        if (o instanceof String) {
            return List.of(((String) o).split(","));
        }

        throw new IllegalArgumentException("Unsupported list object type: " + o.getClass());
    }

    public static <K, V> V get(Map<K, V> m, K key) {
        if (key == null) {
            return null;
        }
        return m.get(key);
    }

    public static <K, V> V getOrDefault(Map<K, V> m, K key, V defaultValue) {
        if (key == null) {
            return defaultValue;
        }
        return m.getOrDefault(key, defaultValue);
    }

    @SafeVarargs
    public static <T> List<T> concat(List<T> ... lists) {
        return Arrays.stream(lists)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @SafeVarargs
    public static <T> Set<T> concatAsSet(List<T> ... lists) {
        return Arrays.stream(lists)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
    }

    public static <T> List<T> reverse(List<T> list) {
        return Lists.reverse(list);
    }

    public static List<Integer> newList(int size) {
        List<Integer> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(i);
        }
        return result;
    }

    public static Map<String, Object> sortByKey(Map<String, Object> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
    }}
