package ca.vanzyl.ck8s.preview;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class PlainTextReportWriter {

    private static final String CREATE_SYMBOL = "+";
    private static final String DELETE_SYMBOL = "-";
    private static final String UPDATE_SYMBOL = "~";

    private final ProcessInfo processInfo;
    private final List<Change> changes;

    public PlainTextReportWriter(ProcessInfo processInfo, List<Change> changes) {
        this.processInfo = processInfo;
        this.changes = normalizeChanges(changes);
    }

    public void write(OutputStream out) throws IOException {
        try (var writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out)))) {

            formatPreviewHeader(processInfo, writer);

            formatPreview(changes, writer);

            formatPreviewFooter(changes, writer);
        }
    }

    private static void formatPreviewHeader(ProcessInfo processInfo, PrintWriter writer) {
        writer.printf("Previewing '%s' flow (on '%s' cluster):\n", processInfo.flow(), processInfo.cluster());

        writer.println("\nwith arguments:");
        if (processInfo.args().isEmpty()) {
            writer.println("    (no arguments provided)");
        } else {
            for (var entry : processInfo.args().entrySet()) {
                writer.printf("    %s: %s\n", entry.getKey(), entry.getValue());
            }
        }
    }

    private static void formatPreview(List<Change> changes, PrintWriter writer) {
        var groupedByParent = changes.stream()
                .filter(change -> change.parentId() != null)
                .collect(Collectors.groupingBy(
                        Change::parentId,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparing(Change::timestamp))
                                        .toList()
                        )
                ));

        var existingIds = changes.stream().map(Change::id).collect(Collectors.toSet());

        var rootChanges = changes.stream()
                .filter(change -> change.parentId() == null || !existingIds.contains(change.parentId()))
                .sorted(Comparator.comparing(Change::timestamp))
                .toList();

        writer.printf("\n    %-40s %-50s %s\n", "Type", "Name", "Plan");

        formatChanges(rootChanges, groupedByParent, 0, writer);
    }

    private static void formatChanges(List<Change> changes, Map<String, List<Change>> groupedByParent, int depth, PrintWriter writer) {
        var isRoot = depth == 0;

        for (int i = 0; i < changes.size(); i++) {
            var change = changes.get(i);

            var action = change.action().name().toLowerCase();
            var type = change.type();
            var name = change.metadata().name() == null ? "" : change.metadata().name();
            var symbol = actionSymbol(change.action());

            var isLast = (i == changes.size() - 1);

            var indent = depth <= 1 ? "" : String.format("%" + ((depth - 1) * 3) + "s", " ");
            var treePrefix = isRoot ? "" : (isLast ? "└─ " : "├─ ");
            var treePrefixWithType = indent + treePrefix + type;

            if (change.error() != null) {
                action = action + " (" + change.error() + ")";
            }

            List<String> wrappedNameLines = wrapText(name, 50);

            for (int j = 0; j < wrappedNameLines.size(); j++) {
                var currentNameLine = wrappedNameLines.get(j);
                if (j == 0) {
                    writer.printf("%s   %-40s %-50s %s\n", symbol, treePrefixWithType, currentNameLine, action);
                } else {
                    writer.printf("    %-40s %-50s\n", "", currentNameLine);
                }
            }

            List<Change> children = groupedByParent.getOrDefault(change.id(), List.of());
            if (!children.isEmpty()) {
                formatChanges(children, groupedByParent, depth + 1, writer);
            }
        }
    }

    /**
     CREATE -> UPDATE -> DELETE -> remove (i.e. no final change)
     CREATE -> DELETE -> remove
     CREATE -> UPDATE(s) -> CREATE
     UPDATE -> DELETE -> DELETE
     Multiple UPDATEs -> UPDATE
     Only CREATE -> CREATE
     Only DELETE -> DELETE
     */
    static List<Change> normalizeChanges(List<Change> changes) {
        Map<String, List<Change>> grouped = changes.stream()
                .collect(Collectors.groupingBy(Change::id));

        List<Change> normalized = grouped.values().stream()
                .map(PlainTextReportWriter::normalizeChangeGroup)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(Change::timestamp))
                .toList();

        return normalized;
    }

    private static Optional<Change> normalizeChangeGroup(List<Change> group) {
        // sort by timestamp (ascending)
        List<Change> sorted = group.stream()
                .sorted(Comparator.comparing(Change::timestamp))
                .toList();

        boolean hasCreate = sorted.stream().anyMatch(c -> c.action() == Change.Action.CREATE);
        boolean hasDelete = sorted.stream().anyMatch(c -> c.action() == Change.Action.DELETE);

        if (hasCreate && hasDelete) {
            // Get the index of first CREATE and last DELETE
            int firstCreateIndex = -1;
            int lastDeleteIndex = -1;
            for (int i = 0; i < sorted.size(); i++) {
                if (sorted.get(i).action() == Change.Action.CREATE && firstCreateIndex == -1) {
                    firstCreateIndex = i;
                }
                if (sorted.get(i).action() == Change.Action.DELETE) {
                    lastDeleteIndex = i;
                }
            }

            if (firstCreateIndex < lastDeleteIndex) {
                // CREATE followed by DELETE -> remove
                return Optional.empty();
            } else {
                // DELETE came first, then CREATE (treat as new CREATE)
                return Optional.of(sorted.get(sorted.size() - 1))
                        .map(c -> Change.builder().from(c).action(Change.Action.CREATE).build());
            }
        }

        if (hasCreate) {
            // Only CREATE and possibly UPDATE(s) -> return last as CREATE
            Change last = sorted.get(sorted.size() - 1);
            return Optional.of(Change.builder().from(last).action(Change.Action.CREATE).build());
        }

        if (hasDelete) {
            // UPDATE -> DELETE -> return last as DELETE
            Change last = sorted.get(sorted.size() - 1);
            return Optional.of(Change.builder().from(last).action(Change.Action.DELETE).build());
        }

        // Only UPDATE(s) -> return last as UPDATE
        Change last = sorted.get(sorted.size() - 1);
        return Optional.of(Change.builder().from(last).action(Change.Action.UPDATE).build());
    }

//    private static Set<ChangeWithUniqueId> findOldChanges(ChangeWithUniqueId change, Map<String, List<ChangeWithUniqueId>> changesById) {
//        var oldChanges = changesById.getOrDefault(change.value.id(), List.of()).stream()
//                .filter(c -> c.value.timestamp() < change.value.timestamp())
//                .toList();
//
//        var result = new HashSet<>(oldChanges);
//        for (var c : oldChanges) {
//            result.addAll(findOldChanges(c, changesById));
//        }
//        return result;
//    }

    private static void formatPreviewFooter(List<Change> changes, PrintWriter writer) {
        int createCount = 0;
        int updateCount = 0;
        int deleteCount = 0;

        for (var change : changes) {
            switch (change.action()) {
                case CREATE -> createCount++;
                case UPDATE -> updateCount++;
                case DELETE -> deleteCount++;
            }
        }

        writer.println("\nResources:");
        writer.printf("    + %d to create\n", createCount);
        writer.printf("    ~ %d to update\n", updateCount);
        writer.printf("    - %d to delete\n", deleteCount);
    }

    private static List<String> wrapText(String text, int maxWidth) {
        var lines = new ArrayList<String>();
        var index = 0;
        while (index < text.length()) {
            var end = Math.min(index + maxWidth, text.length());
            lines.add(text.substring(index, end));
            index = end;
        }
        return lines;
    }

    private static String actionSymbol(Change.Action action) {
        return switch (action) {
            case CREATE -> CREATE_SYMBOL;
            case DELETE -> DELETE_SYMBOL;
            case UPDATE -> UPDATE_SYMBOL;
        };
    }
}
