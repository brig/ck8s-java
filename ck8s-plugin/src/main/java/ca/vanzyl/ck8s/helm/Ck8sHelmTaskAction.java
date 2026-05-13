package ca.vanzyl.ck8s.helm;

import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;

import javax.inject.Named;

public abstract class Ck8sHelmTaskAction<T extends Ck8sHelmTaskParams> {

    public abstract TaskResult execute(Context context, T input) throws Exception;

    public enum Action {
        STATUS
    }
}
