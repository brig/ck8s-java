package ca.vanzyl.ck8s.helm;

import ca.vanzyl.ck8s.helm.actions.StatusAction;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

@Named("ck8sHelm")
public class Ck8sHelmTask implements Task {

    private final Map<Ck8sHelmTaskAction.Action, Ck8sHelmTaskAction<? extends Ck8sHelmTaskParams>> actions = Map.of(
            Ck8sHelmTaskAction.Action.STATUS, new StatusAction()
    );

    private final Context context;

    @Inject
    public Ck8sHelmTask(Context context) {
        this.context = context;
    }

    @Override
    public TaskResult execute(Variables input) throws Exception {
        var variablesMap = new HashMap<>(Optional.ofNullable(context.defaultVariables().toMap()).orElseGet(Map::of));
        variablesMap.putAll(input.toMap());
        var variables = new MapBackedVariables(variablesMap);

        var action = action(variables);
        return actions.get(action).execute(context, VariablesCk8sHelmTaskParams.params(action, context, variables));
    }

    private static Ck8sHelmTaskAction.Action action(Variables variables) {
        var action = variables.assertString("action").trim();
        try {
            return Ck8sHelmTaskAction.Action.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown action: '" + action + "'. Available actions: " + Arrays.toString(Ck8sHelmTaskAction.Action.values()));
        }
    }
}
