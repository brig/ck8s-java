package ca.vanzyl.ck8s.cloudformation;

import java.util.*;

public class StatementMergeStrategy {

    public Optional<Statement> tryMerge(Statement existing, Statement incoming) {
        if (!existing.effect().equals(incoming.effect())) {
            return Optional.empty();
        }

        var existingResources = existing.resources();
        var incomingResources = incoming.resources();

        if (existingResources.containsAll(incomingResources)) {
            var mergedActions = new HashSet<>(existing.actions());
            mergedActions.addAll(incoming.actions());

            var merged = new Statement(
                    existing.effect(),
                    mergedActions,
                    existingResources
            );

            return Optional.of(merged);
        }

        return Optional.empty();
    }
}
