package ca.vanzyl.ck8s.actions;

import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public class DryRunAction<T extends ActionInput, A extends Enum<A> & ActionName> implements TaskAction<T, A> {

    private static final Logger log = LoggerFactory.getLogger(DryRunAction.class);

    private final A action;
    private final Map<String, Object> result;

    public DryRunAction(A action) {
        this(action, Map.of());
    }

    public DryRunAction(A action, Map<String, Object> result) {
        this.action = action;
        this.result = result;
    }

    @Override
    public A action() {
        return action;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, T input) throws Exception {
        log.info("Running in dry-run mode: Skipping execution of action '{}'", action);
        return TaskResult.success()
                .values(result);
    }
}
