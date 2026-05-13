package ca.vanzyl.ck8s.actions;

import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class TaskActionExecutor {

    public static <A extends Enum<A> & ActionName> TaskResult execute(Context context,
                                                                      Variables input,
                                                                      Class<A> actionClass,
                                                                      List<? extends TaskAction<? extends ActionInput, A>> taskActions,
                                                                      BiFunction<A, Variables, ? extends ActionInput> inputConverter) throws Exception {

        var variablesMap = new HashMap<>(Optional.ofNullable(context.defaultVariables().toMap()).orElseGet(Map::of));
        variablesMap.putAll(input.toMap());

        var variables = new MapBackedVariables(variablesMap);
        var actionName = ActionName.fromValue(actionClass, variables.assertString("action"));
        var taskAction = select(context, actionName, taskActions);
        var actionInput = inputConverter.apply(actionName, variables);
        return taskAction.execute(context, actionInput);
    }

    private static <A extends Enum<A> & ActionName> TaskAction<ActionInput, A> select(
            Context context,
            A actionName,
            List<? extends TaskAction<? extends ActionInput, A>> taskActions) {

        if (context.processConfiguration().dryRun()) {
            var phase = DryRunPhases.assertPhase(context);

            // Try to find an action that supports the current dry-run phase
            var action = selectTaskAction(actionName, taskActions,
                    task -> task.dryRunPhases().contains(phase));
            if (action.isPresent()) {
                return action.get();
            }

            // If no action was found and the phase is not the generic DRY_RUN,
            // attempt to find an action that supports the generic DRY_RUN phase.
            if (phase != DryRunPhase.DRY_RUN) {
                return selectTaskAction(actionName, taskActions,
                        task -> task.dryRunPhases().contains(DryRunPhase.DRY_RUN))
                        .orElseGet(() -> new DryRunAction<>(actionName));
            }

            // Default fallback: do nothing DryRun action
            return new DryRunAction<>(actionName);
        }

        // Prefer tasks without dryRunPhases first (regular execution)
        // If none found, use tasks with the DRY_RUN phase
        return selectTaskAction(actionName, taskActions, task -> task.dryRunPhases().isEmpty())
                .or(() -> selectTaskAction(actionName, taskActions, task -> task.dryRunPhases().contains(DryRunPhase.DRY_RUN)))
                .orElseThrow(() -> new RuntimeException("Can't find tasks for action '" + actionName + "': " + taskActions));
    }

    @SuppressWarnings("unchecked")
    private static <A extends Enum<A> & ActionName>  Optional<TaskAction<ActionInput, A>> selectTaskAction(
            ActionName actionName,
            List<? extends TaskAction<? extends ActionInput, A>> taskActions,
            Predicate<TaskAction<?, A>> filter) {

        List<? extends TaskAction<?, A>> filtered = taskActions.stream()
                .filter(task -> actionName.equals(task.action()))
                .filter(filter)
                .toList();

        if (filtered.size() > 1) {
            throw new RuntimeException("Too many tasks for action '" + actionName + "': " + taskActions);
        }

        return filtered.isEmpty() ? Optional.empty() : Optional.of((TaskAction<ActionInput, A>) filtered.get(0));
    }

    private TaskActionExecutor() {
    }
}
