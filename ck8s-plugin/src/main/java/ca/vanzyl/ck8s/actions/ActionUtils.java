package ca.vanzyl.ck8s.actions;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public final class ActionUtils {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <A extends Enum<A> & ActionName, T extends TaskAction<?, A>> List<T> assertActions(
            List<T> taskActions) {

        if (taskActions.isEmpty()) {
            throw new IllegalStateException("No tasks actions provided");
        }

        var allActions = ActionName.knownValues(taskActions.get(0).action().getClass());
        var usedActions = taskActions.stream().map(TaskAction::action).collect(Collectors.toSet());
        var missedTaskActions = new HashSet<>(allActions);
        missedTaskActions.removeAll(usedActions);
        if (!missedTaskActions.isEmpty()) {
            throw new IllegalStateException("Unused task actions: " + missedTaskActions + ". Probably action impl missed");
        }

        return taskActions;
    }

    private ActionUtils() {
    }
}
