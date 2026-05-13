package ca.vanzyl.ck8s.aws.efs.actions;

import ca.vanzyl.ck8s.aws.efs.EfsClientFactory;
import ca.vanzyl.ck8s.aws.efs.EfsTaskAction;
import ca.vanzyl.ck8s.aws.efs.EfsTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.FileSystemNotFoundException;

public class CreateAccessPointAction extends EfsTaskAction<EfsTaskParams.CreateAccessPointParams> {

    private static final Logger log = LoggerFactory.getLogger(CreateAccessPointAction.class);

    @Inject
    public CreateAccessPointAction(EfsClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.CREATE_ACCESS_POINT;
    }

    @Override
    public TaskResult execute(Context context, EfsTaskParams.CreateAccessPointParams input) throws Exception {
        var efsId = input.efsId();
        var rootDirectory = input.rootDirectory();
        var tags = input.tags();
        var name = input.name();

        try (var client = createClient(input)) {
            var existing = FindAccessPointAction.findAccessPoint(client, efsId, name);
            if (existing != null) {
                log.info("✅ Access point '{}' already exists (id: {}). Do nothing...", name, existing.accessPointId());
                return TaskResult.success()
                        .value("id", existing.accessPointId());
            } else {
                log.info("Access point '{}' does not exists. Creating it...", name);

                var response = client.createAccessPoint(r -> r.fileSystemId(efsId)
                        .rootDirectory(rootDirectory)
                        .tags(tags));

                log.info("✅ Access point '{}' created for efs '{}' -> id: '{}'", name, efsId, response.accessPointId());

                return TaskResult.success()
                        .value("id", response.accessPointId());
            }
        } catch (FileSystemNotFoundException e) {
            log.info("File system with id '{}' not found", efsId);
            return TaskResult.success();
        }
    }
}
