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

import static ca.vanzyl.ck8s.jira.Ck8sJiraTaskParams.GetVersionParams;

public class GetVersionAction extends Ck8sJiraTaskAction<GetVersionParams> {

    private final static Logger log = LoggerFactory.getLogger(GetVersionAction.class);

    @Override
    public Action action() {
        return Action.GET_VERSION;
    }

    @Override
    public TaskResult execute(Context context, GetVersionParams input) throws Exception {
        try {
            var version = versionByName(input.baseParams(), input.projectId(), input.name());

            log.info("✅ Successfully get version '{}' -> {}", input.name(), version);

            return TaskResult.success()
                    .value("version", version);
        } catch (Exception e) {
            log.error("❌ Failed to get version '{}'", input.name(), e);
            return TaskResult.fail(e);
        }
    }

    public static Map<String, Object> versionByName(Ck8sJiraTaskParams.BaseParams params, long projectId, String name) throws IOException {
        var versions = JiraClientFactory.create(params.clientCfg())
                .url(params.baseUrl() + "project/" + projectId + "/versions")
                .jiraAuth(params.credentials().authHeaderValue())
                .successCode(200)
                .getList();

        if (versions == null) {
            return null;
        }

        return versions.stream()
                .filter(v -> name.equals(v.get("name")))
                .findFirst()
                .orElse(null);
    }
}
