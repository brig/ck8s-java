package ca.vanzyl.ck8s.utils;

import com.walmartlabs.concord.runtime.v2.sdk.Task;

import javax.inject.Named;
import java.util.*;
import java.util.function.Function;

@Named("table")
public class TableViewTask implements Task {

    public Map<String, List<String>> transformImages(List<Collection<String>> columns) {
        return transformImages(columns, Collections.emptyList());
    }

    public Map<String, List<String>> transformImages(List<Collection<String>> columns, List<Collection<String>> allItems) {
        return transform(columns, i -> K8sUtilsTask.parseImage(i).getRepository(), i -> K8sUtilsTask.parseImage(i).getTag(), allItems);
    }

    public Map<String, List<String>> transformWithSplit(List<Collection<String>> columns, String separator) {
        return transform(columns, s -> s.split(separator, 2)[0], s -> s.split(separator, 2)[1], Collections.emptyList());
    }

    /**
     * transforms:
     * [[mac:3d0f396], [mac:123456]]
     * into:
     * mac -> [3d0f396, 123456]
     */
    public Map<String, List<String>> transform(List<Collection<String>> columns, Function<String, String> keyConverter, Function<String, String> valueConverter) {
        return transform(columns, keyConverter, valueConverter, Collections.emptyList());
    }

    public Map<String, List<String>> transform(List<Collection<String>> columns, Function<String, String> keyConverter, Function<String, String> valueConverter, List<Collection<String>> allItems) {
        Map<String, List<String>> result = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            Collection<String> rows = columns.get(i);
            for (String r : rows) {
                String key = keyConverter.apply(r);
                String value = valueConverter.apply(r);

                List<String> keyValues = result.computeIfAbsent(key, k -> new ArrayList<>(Collections.nCopies(columns.size(), null)));
                if (keyValues.get(i) != null) {
                    throw new RuntimeException("Duplicate key '" + key + "', with value' " + value + "'");
                }

                keyValues.set(i, value);
            }

        }

        if (allItems.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, List<String>> e : result.entrySet()) {
            String key = e.getKey();
            for (int i = 0; i < e.getValue().size(); i++) {
                if (e.getValue().get(i) == null) {
                    String value = allItems.get(i).stream()
                            .filter(item -> key.equals(keyConverter.apply(item)))
                            .findFirst()
                            .map(valueConverter)
                            .orElse(null);

                    e.getValue().set(i, value);
                }
            }
        }

        return result;
    }

    public static String toStringN(Map<String, List<String>> m, String ... caption) {
        return toStringN(m, Arrays.asList(caption));
    }

    public static String toStringN(Map<String, List<String>> m, List<String> captions) {
        return "\n" + toString(m, captions);
    }

    public static String toString(Map<String, List<String>> m, String ... caption) {
        return toString(m, Arrays.asList(caption));
    }

    public static String toString(Map<String, List<String>> m, List<String> captions) {
        for (Map.Entry<String, List<String>> e : m.entrySet()) {
            if (e.getValue().size() != captions.size() - 1) {
                throw new IllegalArgumentException("Expected " + (captions.size() - 1) + " cells in row: " + e);
            }
        }

        List<List<String>> rows = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : m.entrySet()) {
            List<String> r = new ArrayList<>(e.getValue());
            r.add(0, e.getKey());
            rows.add(r);
        }

        rows.add(0, captions);

        return toString(rows);
    }

    public static String toString(List<List<String>> rows) {
        if (rows.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        String formatString = buildFormatString(maxRow(rows));
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            result.append(formatNull(formatString, row));
            if (i < rows.size() - 1){
                result.append('\n');
            }
        }

        return result.toString();
    }

    private static String buildFormatString(int[] row) {
        StringBuilder result = new StringBuilder();
        for (int c : row) {
            result.append("%").append(c).append("s ");
        }
        return result.toString();
    }

    private static int[] maxRow(List<List<String>> rows) {
        int[] result = new int[rows.get(0).size()];
        for (List<String> r : rows) {
            for (int i = 0; i < r.size(); i++) {
                int len = r.get(i) != null ? r.get(i).length() + 2 : 5;
                if (len > result[i]) {
                    result[i] = len;
                }
            }
        }
        return result;
    }

    private static String formatNull(String str, List<String> args) {
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i) == null) {
                args.set(i, "n/a");
            }
        }

        return String.format(str, args.toArray());
    }
}
