package ca.vanzyl.ck8s.jira;

import ca.vanzyl.ck8s.actions.ActionInput;
import com.walmartlabs.concord.plugins.jira.JiraClientCfg;
import com.walmartlabs.concord.plugins.jira.JiraCredentials;

import java.util.Map;

public interface Ck8sJiraTaskParams extends ActionInput {

    BaseParams baseParams();

    record BaseParams(JiraClientCfg clientCfg, JiraCredentials credentials, String baseUrl) {
    }

    record CreateVersionParams(
            BaseParams baseParams,
            long projectId,
            String name,
            String description,
            String releaseDate,
            Boolean released,
            Boolean archived
    ) implements Ck8sJiraTaskParams {
    }

    record GetVersionParams(
            BaseParams baseParams,
            long projectId,
            String name
    ) implements Ck8sJiraTaskParams {
    }

    record EditIssueParams(
        BaseParams baseParams,
        String issueKey,
        Map<String, Object> fields,
        Map<String, Object> update
    ) implements Ck8sJiraTaskParams {
    }

    record GetIssueParams(
            BaseParams baseParams,
            String issueKey
    ) implements Ck8sJiraTaskParams {
    }
}
