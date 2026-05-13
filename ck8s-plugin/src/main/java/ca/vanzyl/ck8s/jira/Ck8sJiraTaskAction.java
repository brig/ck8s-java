package ca.vanzyl.ck8s.jira;

import ca.vanzyl.ck8s.actions.ActionName;
import ca.vanzyl.ck8s.actions.TaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;

public abstract class Ck8sJiraTaskAction<T extends Ck8sJiraTaskParams> implements TaskAction<T, Ck8sJiraTaskAction.Action> {

    public abstract Action action();

    public abstract TaskResult execute(Context context, T input) throws Exception;

    protected Ck8sJiraHttpClient createClient(Ck8sJiraTaskParams input) {
        return createClient(input.baseParams());
    }

    protected Ck8sJiraHttpClient createClient(Ck8sJiraTaskParams.BaseParams params) {
        return JiraClientFactory.create(params.clientCfg());
    }

    public enum Action implements ActionName {
        GET_VERSION("get-version"),
        CREATE_VERSION("create-version"),
        UPDATE_VERSION("update-version"),
        UPSERT_VERSION("upsert-version"),
        GET_ISSUE("get-issue"),
        EDIT_ISSUE("edit-issue");

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
