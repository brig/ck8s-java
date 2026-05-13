package ca.vanzyl.ck8s.jira.actions;

import ca.vanzyl.ck8s.jira.Ck8sJiraTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static ca.vanzyl.ck8s.jira.Ck8sJiraTaskParams.CreateVersionParams;
import static ca.vanzyl.ck8s.jira.Utils.putIfNotNull;
import static ca.vanzyl.ck8s.jira.actions.CreateVersionAction.createVersion;
import static ca.vanzyl.ck8s.jira.actions.UpdateVersionAction.findVersionId;
import static ca.vanzyl.ck8s.jira.actions.UpdateVersionAction.updateVersion;

public class UpsertVersionAction extends Ck8sJiraTaskAction<CreateVersionParams> {

    private final static Logger log = LoggerFactory.getLogger(UpsertVersionAction.class);

    @Override
    public Action action() {
        return Action.UPSERT_VERSION;
    }

    @Override
    public TaskResult execute(Context context, CreateVersionParams input) throws Exception {
        try {
            var versionId = findVersionId(input.baseParams(), input.projectId(), input.name());
            if (versionId != null) {
                log.info("Version '{}' exists. Updating it...", input.name());

                updateVersion(versionId, input);

                log.info("✅ Successfully updated version '{}' -> {} id", input.name(), versionId);
            } else {
                log.info("Version '{}' does not exists. Creating it...", input.name());

                versionId = createVersion(input);

                log.info("✅ Successfully created version '{}' -> {} id", input.name(), versionId);
            }

            return TaskResult.success()
                    .value("id", versionId);
        } catch (Exception e) {
            log.error("❌ Failed to upsert version '{}'", input.name(), e);
            return TaskResult.fail(e);
        }
    }
}
