package ca.vanzyl.ck8s.actions;

import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;

import java.util.Set;

public interface TaskAction<T, A extends Enum<A> & ActionName> {

    A action();

    default Set<DryRunPhase> dryRunPhases() {
        return Set.of();
    }

    TaskResult execute(Context context, T input) throws Exception;
}
