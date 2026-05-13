package ca.vanzyl.ck8s.jira.actions;

import ca.vanzyl.ck8s.jira.Ck8sJiraTaskAction;
import ca.vanzyl.ck8s.jira.JiraClientFactory;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static ca.vanzyl.ck8s.jira.Ck8sJiraTaskParams.CreateVersionParams;
import static ca.vanzyl.ck8s.jira.Utils.*;

public class CreateVersionAction extends Ck8sJiraTaskAction<CreateVersionParams> {

    private final static Logger log = LoggerFactory.getLogger(CreateVersionAction.class);

    @Override
    public Action action() {
        return Action.CREATE_VERSION;
    }

    @Override
    public TaskResult execute(Context context, CreateVersionParams input) throws Exception {
        try {
            var versionId = createVersion(input);

            log.info("✅ Successfully created version '{}' -> {} id", input.name(), versionId);

            return TaskResult.success()
                    .value("id", versionId);
        } catch (Exception e) {
            log.error("❌ Failed to create version '{}'", input.name(), e);
            return TaskResult.fail(e);
        }
    }

    public static String createVersion(CreateVersionParams input) throws Exception {
        var payload = new HashMap<String, Object>();
        payload.put("projectId", input.projectId());
        payload.put("name", input.name());

        putIfNotNull(payload, "description", input.description());
        putIfNotNull(payload, "releaseDate", input.releaseDate());
        putIfNotNull(payload, "archived", input.archived());
        putIfNotNull(payload, "released", input.released());

        var result = JiraClientFactory.create(input.baseParams().clientCfg())
                .url(input.baseParams().baseUrl() + "version")
                .jiraAuth(input.baseParams().credentials().authHeaderValue())
                .successCode(201)
                .post(payload);

        return (String)result.get("id");
    }
}
