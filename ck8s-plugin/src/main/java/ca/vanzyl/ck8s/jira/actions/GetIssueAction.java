package ca.vanzyl.ck8s.jira.actions;

import ca.vanzyl.ck8s.jira.Ck8sJiraHttpClient;
import ca.vanzyl.ck8s.jira.Ck8sJiraTaskAction;
import ca.vanzyl.ck8s.jira.Ck8sJiraTaskParams;
import ca.vanzyl.ck8s.jira.JiraClientFactory;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static ca.vanzyl.ck8s.jira.Ck8sJiraTaskParams.GetIssueParams;

public class GetIssueAction extends Ck8sJiraTaskAction<GetIssueParams> {

    private final static Logger log = LoggerFactory.getLogger(GetIssueAction.class);

    @Override
    public Action action() {
        return Action.GET_ISSUE;
    }

    @Override
    public TaskResult execute(Context context, GetIssueParams input) throws Exception {
        var issueKey = input.issueKey();

        try {
            var issue = getIssue(input.baseParams(), issueKey);
            if (issue == null) {
                log.info("Issue '{}' not found", issueKey);
            } else {
                log.info("✅ Successfully get issue '{}'", issueKey);
            }

            return TaskResult.success()
                    .value("issue", issue);
        } catch (Exception e) {
            log.error("❌ Failed to get issue '{}'", issueKey, e);
            return TaskResult.fail(e);
        }
    }

    public static Map<String, Object> getIssue(Ck8sJiraTaskParams.BaseParams params, String issueKey) throws IOException {
        try {
            return JiraClientFactory.create(params.clientCfg())
                    .url(params.baseUrl() + "issue/" + issueKey)
                    .jiraAuth(params.credentials().authHeaderValue())
                    .successCode(200)
                    .get();
        } catch (Ck8sJiraHttpClient.UnexpectedResponseException e) {
            if (e.getCode() == 404) {
                return null;
            }
            throw e;
        }
    }
}
