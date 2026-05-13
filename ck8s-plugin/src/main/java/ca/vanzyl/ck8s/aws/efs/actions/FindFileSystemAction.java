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
import software.amazon.awssdk.services.efs.model.FileSystemDescription;

import javax.inject.Inject;
import java.util.Set;

public class FindFileSystemAction extends EfsTaskAction<EfsTaskParams.FindFileSystemParams> {

    private static final Logger log = LoggerFactory.getLogger(FindFileSystemAction.class);

    @Inject
    public FindFileSystemAction(EfsClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public Action action() {
        return Action.FIND_FILE_SYSTEM;
    }

    @Override
    public TaskResult execute(Context context, EfsTaskParams.FindFileSystemParams input) throws Exception {
        var name = input.name();

        try (var client = createClient(input)) {
            var efs = client.describeFileSystemsPaginator().fileSystems().stream()
                    .filter(FileSystemDescription::hasTags)
                    .filter(fs -> fs.tags().stream()
                            .anyMatch(tag -> tag.key().equals("Name") && tag.value().equals(name)))
                    .toList();

            if (efs.size() > 1) {
                log.error("❌ Multiple file systems ({}) found for name: '{}'", efs.size(), name);
                log.error("File systems: {}", efs);
                throw new RuntimeException("Multiple file systems found for name: '" + name + "'");
            } else if (efs.isEmpty()) {
                return TaskResult.success();
            }

            return TaskResult.success()
                    .value("id", efs.get(0).fileSystemId())
                    .value("fileSystem", AwsTaskUtils.serialize(efs.get(0)));
        }
    }
}
