package ca.vanzyl.ck8s.k8s;

import ca.vanzyl.ck8s.actions.ActionName;
import ca.vanzyl.ck8s.actions.TaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;

public interface K8sTaskAction<T extends K8sTaskParams> extends TaskAction<T, K8sTaskAction.Action> {

    @Override
    TaskResult execute(Context context, T input) throws Exception;

    @Override
    Action action();

    enum Action implements ActionName {
        CREATE_NAMESPACE("create-namespace"),
        DELETE_NAMESPACE("delete-namespace"),
        NAMESPACE_EXISTS("namespace-exists"),
        GET_SECRET_DATA("get-secret-data"),
        GET_PODS("get-pods"),
        UPSERT_SECRET("upsert-secret"),
        LIST_EVENTS("list-events"),
        POD_LOGS("pod-logs");

        private final String value;

        Action(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
