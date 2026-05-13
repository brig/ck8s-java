package ca.vanzyl.ck8s.jira.actions;

import ca.vanzyl.ck8s.jira.Ck8sJiraTaskAction;
import ca.vanzyl.ck8s.jira.Ck8sJiraTaskParams;
import ca.vanzyl.ck8s.jira.JiraClientFactory;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;

import static ca.vanzyl.ck8s.jira.Ck8sJiraTaskParams.CreateVersionParams;
import static ca.vanzyl.ck8s.jira.Utils.putIfNotNull;
import static ca.vanzyl.ck8s.jira.actions.GetVersionAction.versionByName;

public class UpdateVersionAction extends Ck8sJiraTaskAction<CreateVersionParams> {

    private final static Logger log = LoggerFactory.getLogger(UpdateVersionAction.class);

    @Override
    public Action action() {
        return Action.UPDATE_VERSION;
    }

    @Override
    public TaskResult execute(Context context, CreateVersionParams input) throws Exception {
        var versionId = findVersionId(input.baseParams(), input.projectId(), input.name());
        if (versionId == null) {
            throw new UserDefinedException("Jira version '" + input.name() + "' not found in project '" + input.projectId() + "'");
        }

        try {
            updateVersion(versionId, input);

            log.info("✅ Successfully updated version '{}'", input.name());

            return TaskResult.success()
                    .value("id", versionId);
        } catch (Exception e) {
            log.error("❌ Failed to update version '{}'", input.name(), e);
            return TaskResult.fail(e);
        }
    }

    public static void updateVersion(String versionId, CreateVersionParams input) throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("projectId", input.projectId());
        payload.put("name", input.name());

        putIfNotNull(payload, "description", input.description());
        putIfNotNull(payload, "releaseDate", input.releaseDate());
        putIfNotNull(payload, "archived", input.archived());
        putIfNotNull(payload, "released", input.released());

        log.info("payload: {}", payload);

        JiraClientFactory.create(input.baseParams().clientCfg())
                .url(input.baseParams().baseUrl() + "version/" + versionId)
                .jiraAuth(input.baseParams().credentials().authHeaderValue())
                .successCode(200)
                .put(payload);
    }

    public static String findVersionId(Ck8sJiraTaskParams.BaseParams params, long projectId, String name) throws IOException {
        var version = versionByName(params, projectId, name);
        if (version == null) {
            return null;
        }
        return (String)version.get("id");
    }
}
