package ca.vanzyl.ck8s.aws.efs.actions;

import ca.vanzyl.ck8s.aws.efs.EfsClientFactory;
import ca.vanzyl.ck8s.aws.efs.EfsTaskAction;
import ca.vanzyl.ck8s.aws.efs.EfsTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.efs.model.AccessPointNotFoundException;

import javax.inject.Inject;

public class DeleteAccessPointAction extends EfsTaskAction<EfsTaskParams.DeleteAccessPointParams> {

    private static final Logger log = LoggerFactory.getLogger(DeleteAccessPointAction.class);

    @Inject
    public DeleteAccessPointAction(EfsClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.DELETE_ACCESS_POINT;
    }

    @Override
    public TaskResult execute(Context context, EfsTaskParams.DeleteAccessPointParams input) throws Exception {
        var efsId = input.efsId();
        var name = input.name();

        try (var client = createClient(input)) {
            var accessPoint = FindAccessPointAction.findAccessPoint(client, efsId, name);
            if (accessPoint == null) {
                log.info("✅ Access point '{}' does not exist", name);
                return TaskResult.success();
            }

            client.deleteAccessPoint(r -> r.accessPointId(accessPoint.accessPointId()));

            log.info("✅ Access point '{}' deleted successfully", name);

            return TaskResult.success();
        } catch (AccessPointNotFoundException e) {
            log.info("Access point '{}' not found", name);
            return TaskResult.success();
        }
    }
}
