package ca.vanzyl.ck8s.jira;

import com.walmartlabs.concord.plugins.jira.JiraClientCfg;
import com.walmartlabs.concord.plugins.jira.JiraCredentials;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import static ca.vanzyl.ck8s.jira.Ck8sJiraTaskParams.*;

public final class VariablesCk8sJiraTaskParams {

    private static final String PROJECT_ID_KEY = "projectId";
    private static final String ISSUE_KEY = "issueKey";

    public static CreateVersionParams createVersion(Context context, Variables variables) {
        return new CreateVersionParams(
                baseParams(context, variables),
                assertProjectId(variables),
                variables.assertString("name"),
                variables.getString("description"),
                variables.getString("releaseDate"),
                variables.getBoolean("released", false),
                variables.getBoolean("archived", false)
        );
    }

    public static GetVersionParams getVersion(Context context, Variables variables) {
        return new GetVersionParams(
                baseParams(context, variables),
                assertProjectId(variables),
                variables.assertString("name")
        );
    }

    private static BaseParams baseParams(Context context, Variables variables) {
        return new BaseParams(
                new JiraClientCfg() {
                },
                new JiraCredentials(variables.assertString("userId"), variables.assertString("password")),
                variables.assertString("apiUrl")
        );
    }

    public static EditIssueParams editIssue(Context context, Variables variables) {
        return new EditIssueParams(
                baseParams(context, variables),
                assertIssueKey(variables),
                variables.getMap("fields", null),
                variables.getMap("update", null)
        );
    }

    public static GetIssueParams getIssue(Context context, Variables variables) {
        return new GetIssueParams(
                baseParams(context, variables),
                assertIssueKey(variables)
        );
    }

    private static long assertProjectId(Variables variables) {
        return variables.assertLong(PROJECT_ID_KEY);
    }

    private static String assertIssueKey(Variables variables) {
        return variables.assertString(ISSUE_KEY);
    }
}
