package ca.vanzyl.ck8s.aws.efs.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.AwsTaskUtils;
import ca.vanzyl.ck8s.aws.efs.EfsClientFactory;
import ca.vanzyl.ck8s.aws.efs.EfsTaskAction;
import ca.vanzyl.ck8s.aws.efs.EfsTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.efs.EfsClient;
import software.amazon.awssdk.services.efs.model.AccessPointDescription;

import javax.inject.Inject;
import java.nio.file.FileSystemNotFoundException;
import java.util.Set;

public class FindAccessPointAction extends EfsTaskAction<EfsTaskParams.FindAccessPointParams> {

    private static final Logger log = LoggerFactory.getLogger(FindAccessPointAction.class);

    @Inject
    public FindAccessPointAction(EfsClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public Action action() {
        return Action.FIND_ACCESS_POINT;
    }

    @Override
    public TaskResult execute(Context context, EfsTaskParams.FindAccessPointParams input) throws Exception {
        var efsId = input.efsId();
        var name = input.name();

        try (var client = createClient(input)) {
            var accessPoint = findAccessPoint(client, efsId, name);
            if (accessPoint == null) {
                return TaskResult.success();
            }

            return TaskResult.success()
                    .value("id", accessPoint.accessPointId())
                    .value("accessPoint", AwsTaskUtils.serialize(accessPoint));
        }
    }

    public static AccessPointDescription findAccessPoint(EfsClient client, String efsId, String name) {
        try {
            var accessPoints = client.describeAccessPointsPaginator(r -> r.fileSystemId(efsId))
                    .accessPoints().stream().filter(ap -> name.equals(ap.name()))
                    .toList();

            if (accessPoints.size() > 1) {
                log.error("❌ Multiple access points ({}) found for name: '{}'", accessPoints.size(), name);
                log.error("Access points: {}", accessPoints);
                throw new RuntimeException("Multiple access points found for name: " + name);
            } else if (accessPoints.isEmpty()) {
                return null;
            }

            return accessPoints.get(0);
        } catch (FileSystemNotFoundException e) {
            log.info("File system with id '{}' not found", efsId);
            return null;
        }
    }
}
