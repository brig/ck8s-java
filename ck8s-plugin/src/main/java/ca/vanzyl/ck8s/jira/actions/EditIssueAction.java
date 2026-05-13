package ca.vanzyl.ck8s.jira.actions;

import ca.vanzyl.ck8s.jira.Ck8sJiraHttpClient;
import ca.vanzyl.ck8s.jira.Ck8sJiraTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static ca.vanzyl.ck8s.jira.Ck8sJiraTaskParams.EditIssueParams;
import static ca.vanzyl.ck8s.jira.Utils.putIfNotNull;

public class EditIssueAction extends Ck8sJiraTaskAction<EditIssueParams> {

    private final static Logger log = LoggerFactory.getLogger(EditIssueAction.class);

    @Override
    public Action action() {
        return Action.EDIT_ISSUE;
    }

    @Override
    public TaskResult execute(Context context, EditIssueParams input) throws Exception {
        var issueKey = input.issueKey();

        var payload = new HashMap<String, Object>();
        putIfNotNull(payload, "fields", input.fields());
        putIfNotNull(payload, "update", input.update());

        try {
            createClient(input)
                    .url(input.baseParams().baseUrl() + "issue/" + issueKey)
                    .jiraAuth(input.baseParams().credentials().authHeaderValue())
                    .successCode(204)
                    .put(payload);

            log.info("✅ Successfully edit issue '{}'", issueKey);

            return TaskResult.success()
                    .value("updated", true);
        } catch (Ck8sJiraHttpClient.UnexpectedResponseException e) {
            if (e.getCode() == 404) {
                log.info("Issue '{}' not found", issueKey);
                return TaskResult.success();
            }
            throw e;
        } catch (Exception e) {
            log.error("❌ Failed to edit issue '{}'", issueKey, e);
            return TaskResult.fail(e);
        }
    }
}
