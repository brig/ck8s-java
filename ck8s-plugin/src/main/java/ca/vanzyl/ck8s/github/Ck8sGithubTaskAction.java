package ca.vanzyl.ck8s.github;

import ca.vanzyl.ck8s.actions.ActionName;
import ca.vanzyl.ck8s.actions.TaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.eclipse.egit.github.core.client.GitHubClient;

public abstract class Ck8sGithubTaskAction<T extends Ck8sGithubTaskParams> implements TaskAction<T, Ck8sGithubTaskAction.Action> {

    public abstract Action action();

    public abstract TaskResult execute(Context context, T input) throws Exception;

    protected GitHubClient createClient(Ck8sGithubTaskParams input) {
        return createClient(input.baseParams());
    }

    protected GitHubClient createClient(Ck8sGithubTaskParams.BaseParams params) {
        return GithubClientFactory.create(params.apiUrl());
    }

    public enum Action implements ActionName {

        LIST_COMMITS("list-commits"),
        GET_SHORT_SHA("get-short-sha");

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
