package ca.vanzyl.ck8s.preview;

import ca.vanzyl.ck8s.asserts.json.JsonComparatorV2;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ChangeUtils {

    public static List<Change> jsonChanges(
            String oldDoc, String newDoc,
            Consumer<ImmutableChange.Builder> change) {

        if (oldDoc == null && newDoc == null) {
            return List.of();
        }

        var builder = Change.builder();

        if (oldDoc == null) {
            builder.action(Change.Action.CREATE);
            change.accept(builder);
            return List.of(builder.build());
        }

        if (newDoc == null) {
            builder.action(Change.Action.DELETE);
            change.accept(builder);
            return List.of(builder.build());
        }

        builder.action(Change.Action.UPDATE);

        if (!oldDoc.trim().startsWith("{")) {
            throw new IllegalArgumentException("Expected json document, got: " + oldDoc);
        }

        var result = new JsonComparatorV2().compare(newDoc, oldDoc);
        if (result.success()) {
            return List.of();
        }

        builder.diffMessage(result.message());
        change.accept(builder);

        return List.of(builder.build());
    }

    public static List<Change> mapChanges(String parentId, String changeType,
                                          Map<String, String> oldMap, Map<String, String> newMap,
                                          Function<String, String> idGenerator) {
        if (oldMap.equals(newMap)) {
            return List.of();
        }

        var changes = new ArrayList<Change>();

        Set<String> removedKeys = new HashSet<>(oldMap.keySet());
        removedKeys.removeAll(newMap.keySet());
        for (var r : removedKeys) {
            changes.add(Change.builder()
                    .action(Change.Action.DELETE)
                    .id(idGenerator.apply(r))
                    .type(changeType)
                    .parentId(parentId)
                    .metadata(Change.Metadata.builder().name(r).build())
                    .build());
        }

        Set<String> addedKeys = new HashSet<>(newMap.keySet());
        addedKeys.removeAll(oldMap.keySet());
        for (var r : addedKeys) {
            changes.add(Change.builder()
                    .action(Change.Action.CREATE)
                    .id(idGenerator.apply(r))
                    .type(changeType)
                    .parentId(parentId)
                    .metadata(Change.Metadata.builder().name(r).build())
                    .build());
        }

        Set<String> commonKeys = new HashSet<>(oldMap.keySet());
        commonKeys.retainAll(newMap.keySet());
        for (String key : commonKeys) {
            if (!Objects.equals(oldMap.get(key), newMap.get(key))) {
                changes.add(Change.builder()
                        .action(Change.Action.UPDATE)
                        .id(idGenerator.apply(key))
                        .type(changeType)
                        .parentId(parentId)
                        .metadata(Change.Metadata.builder().name(key).build())
                        .build());
            }
        }

        return changes;
    }

    public static List<Change> listChanges(String parentId, String changeType, Set<String> oldSet, Set<String> newSet,
                                           Function<String, String> idGenerator,
                                           Function<String, String> nameGenerator) {
        if (oldSet.equals(newSet)) {
            return List.of();
        }

        var changes = new ArrayList<Change>();

        var removed = new HashSet<>(oldSet);
        removed.removeAll(newSet);
        for (var r : removed) {
            changes.add(Change.builder()
                    .action(Change.Action.DELETE)
                    .id(idGenerator.apply(r))
                    .type(changeType)
                    .parentId(parentId)
                    .metadata(Change.Metadata.builder().name(nameGenerator.apply(r)).build())
                    .build());
        }

        var added = new HashSet<>(newSet);
        added.removeAll(oldSet);
        for (var r : added) {
            changes.add(Change.builder()
                    .action(Change.Action.CREATE)
                    .id(idGenerator.apply(r))
                    .type(changeType)
                    .parentId(parentId)
                    .metadata(Change.Metadata.builder().name(nameGenerator.apply(r)).build())
                    .build());
        }

        return changes;
    }

    private ChangeUtils() {
    }
}
