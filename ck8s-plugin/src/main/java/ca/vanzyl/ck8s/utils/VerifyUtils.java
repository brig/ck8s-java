package ca.vanzyl.ck8s.utils;

import ca.vanzyl.ck8s.asserts.json.JsonComparatorV2;
import ca.vanzyl.ck8s.aws.AwsTaskUtils;
import ca.vanzyl.ck8s.common.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

import java.util.*;

public final class VerifyUtils {

    private static final Logger log = LoggerFactory.getLogger(VerifyUtils.class);

    private static final int MAX_DIFF_ITEMS_TO_PRINT = 10;

    public static final class LogIcons {
        public static final String OK = "🟢";
        public static final String WARN = "🟡";
        public static final String ERROR = "❌";
    }

    public static void dumpDiff(String entityName, ToCopyableBuilder<?, ?> existing, ToCopyableBuilder<?, ?> expected, List<String> attrs) {
        var existingPoolMap = AwsTaskUtils.serialize(existing);
        existingPoolMap.keySet().retainAll(attrs);
        var newPoolMap = AwsTaskUtils.serialize(expected);
        newPoolMap.keySet().retainAll(attrs);
        var compareResult = new JsonComparatorV2().compare(newPoolMap, existingPoolMap);

        log.info("Existing {}:\n{}", entityName, Mapper.yaml().writeAsString(existingPoolMap));
        log.info("New {}:\n{}", entityName, Mapper.yaml().writeAsString(newPoolMap));
        log.info("Diff:\n{}", compareResult.message());
    }

    public static <T> boolean verifyPartialListMatch(String attributeName, List<T> existing, List<T> expected) {
        return verifyPartialListMatch(attributeName, existing, expected, false);
    }

    @SuppressWarnings("unchecked")
    public static <T> boolean verifyPartialListMatch(String attributeName, List<T> existing, List<T> expected, boolean ignoreMismatch) {
        if (existing == null || expected == null) {
            log.info("{} {}: one of the lists is null (existing={}, expected={})",
                    LogIcons.ERROR, attributeName, existing, expected);
            return false;
        }

        var remaining = new ArrayList<>(existing);
        var missing = new ArrayList<>();

        for (var expectedItem : expected) {
            boolean matched = false;

            if (expectedItem instanceof Map<?, ?> expectedMap) {
                var iterator = remaining.iterator();
                while (iterator.hasNext()) {
                    var actualItem = iterator.next();
                    if (actualItem instanceof Map<?, ?> actualMap) {
                        var mismatchMap = new LinkedHashMap<String, Map<String, Object>>();
                        if (collectPartialMapMismatches(0, attributeName, (Map<String, ?>) actualMap, (Map<String, ?>) expectedMap, mismatchMap)) {
                            matched = true;
                            iterator.remove();
                            break;
                        }
                    }
                }
            } else {
                matched = remaining.remove(expectedItem);
            }

            if (!matched) {
                missing.add(expectedItem);
            }
        }

        if (missing.isEmpty()) {
            if (existing.size() == expected.size()) {
                log.info("{} {} ...ok (exact match)", LogIcons.OK, attributeName);
            } else {
                log.info("{} {} ...partial match (actual: {}, expected: {})",
                        LogIcons.WARN, attributeName, existing.size(), expected.size());
            }
            return true;
        }

        var icon = LogIcons.ERROR;
        if (ignoreMismatch) {
            icon = LogIcons.WARN;
        }
        log.info("{} {} ...mismatch (actual: {}, expected: {}), missing items:",
                icon, attributeName, existing.size(), expected.size());

        for (int i = 0; i < Math.min(MAX_DIFF_ITEMS_TO_PRINT, missing.size()); i++) {
            log.info("\t{}", missing.get(i));
        }
        if (missing.size() > MAX_DIFF_ITEMS_TO_PRINT) {
            log.info("\t...and more (only first {} shown)", MAX_DIFF_ITEMS_TO_PRINT);
        }
        return ignoreMismatch;
    }

    public static boolean verifyPartialMapMatch(String attributeName, ToCopyableBuilder<?, ?> existing, ToCopyableBuilder<?, ?> expected) {
        return verifyPartialMapMatch(attributeName, AwsTaskUtils.serialize(existing), AwsTaskUtils.serialize(expected));
    }

    public static boolean verifyPartialMapMatch(String attributeName, Map<String, ?> existing, Map<String, ?> expected) {
        var mismatches = new LinkedHashMap<String, Map<String, Object>>();

        var valid = collectPartialMapMismatches(0, attributeName, existing, expected, mismatches);
        if (valid) {
            if (Objects.equals(existing, expected)) {
                log.info("{} {} ...ok (exact match)", LogIcons.OK, attributeName);
            } else {
                log.info("{} {} ...ok (partial match, actual: {}, expected: {})",
                        LogIcons.WARN, attributeName, existing.size(), expected.size());

                int printedCount = 0;
                for (var e : existing.entrySet()) {
                    if (!expected.containsKey(e.getKey())) {
                        if (printedCount == 0) {
                            log.info("Unexpected:");
                        }
                        log.warn("\t{}={}", e.getKey(), e.getValue());
                        if (++printedCount > MAX_DIFF_ITEMS_TO_PRINT) {
                            log.warn("\t...and more (only first {} shown)", MAX_DIFF_ITEMS_TO_PRINT);
                            break;
                        }
                    }
                }
            }
        } else {
            log.info("{} {} ...mismatch (actual: {}, expected: {})",
                    LogIcons.ERROR, attributeName,
                    Optional.ofNullable(existing).map(e -> String.valueOf(e.size())).orElse("n/a"),
                    Optional.ofNullable(expected).map(e -> String.valueOf(e.size())).orElse("n/a"));

            mismatches.forEach((key, mismatch) ->
                    log.warn("\tkey '{}':\nexpected:\n'{}'\nfound:\n'{}'",
                            key, mismatch.get("expected"), mismatch.get("actual")));
        }

        return valid;
    }

    public static boolean verifyJsonAttribute(
            String attributeName,
            String existing,
            String expected) {

        var compareResult = new JsonComparatorV2().compare(existing, expected);
        if (compareResult.success()) {
            log.info("{} {} ...ok", LogIcons.OK, attributeName);
            return true;
        }

        log.error("{} {} ...mismatch", LogIcons.ERROR, attributeName);
        log.info("existing:\n{}", dumpJson(existing));
        log.info("new:\n{}", dumpJson(expected));
        log.info("diff: {}", compareResult.message());

        return false;
    }

    public static <T> boolean verifyAttribute(
            String attributeName,
            T existing,
            T expected) {

        return verifyAttribute(attributeName, existing, expected, false);
    }

    public static <T> boolean verifyAttribute(
            String attributeName,
            T existing,
            T expected,
            boolean yamlOutput) {

        if (!Objects.equals(existing, expected)) {
            log.info("{} {} differs", LogIcons.ERROR, attributeName);

            if (yamlOutput) {
                try {
                    log.info("existing:\n{}", Mapper.yaml().writeAsString(existing));
                    log.info("new:\n{}", Mapper.yaml().writeAsString(expected));
                } catch (Exception e) {
                    log.error("{} Failed to render YAML for '{}'", LogIcons.ERROR, attributeName, e);
                }
            } else {
                log.info("\nexisting:\n{}\nnew:\n{}", existing, expected);
            }

            return false;
        } else {
            log.info("{} {} ...ok", LogIcons.OK, attributeName);
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean collectPartialMapMismatches(int depth,
                                                       String attributePath,
                                                       Map<String, ?> existing,
                                                       Map<String, ?> expected,
                                                       Map<String, Map<String, Object>> mismatches) {
        if (existing == null || expected == null) {
            var m = new HashMap<String, Object>();
            m.put("expected", expected);
            m.put("actual", existing);

            mismatches.put(attributePath, m);
            return false;
        }

        boolean valid = true;

        for (var entry : expected.entrySet()) {
            var key = entry.getKey();
            var fullKey = depth == 0 ? key : attributePath + "." + key;
            var expectedValue = entry.getValue();
            var actualValue = existing.get(key);

            if (expectedValue instanceof Map<?, ?> expectedNested &&
                    actualValue instanceof Map<?, ?> actualNested) {

                boolean nestedValid = collectPartialMapMismatches(
                        depth + 1,
                        fullKey,
                        (Map<String, ?>) actualNested,
                        (Map<String, ?>) expectedNested,
                        mismatches);

                if (!nestedValid) valid = false;

            } else if (!Objects.equals(expectedValue, actualValue)) {
                var m = new HashMap<String, Object>();
                m.put("expected", expectedValue);
                m.put("actual", actualValue);
                mismatches.put(fullKey, m);

                valid = false;
            }
        }

        return valid;
    }

    private static String dumpJson(String json) {
        var node = Mapper.json().readNode(json);
        return Mapper.json().writeAsString(node);
    }

    private VerifyUtils() {
    }
}
