package ca.vanzyl.ck8s.aws.glue.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.glue.GlueClientFactory;
import ca.vanzyl.ck8s.aws.glue.GlueTaskAction;
import ca.vanzyl.ck8s.aws.glue.GlueTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.glue.model.EntityNotFoundException;
import software.amazon.awssdk.services.glue.model.GlueException;

import javax.inject.Inject;
import java.util.Set;

public class ExistsDatabaseAction extends GlueTaskAction<GlueTaskParams.ExistsParams> {

    private final static Logger log = LoggerFactory.getLogger(ExistsDatabaseAction.class);

    @Inject
    public ExistsDatabaseAction(GlueClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.EXISTS;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, GlueTaskParams.ExistsParams input) throws Exception {
        var name = input.databaseName();

        try (var client = createClient(input)) {
            var response = client.getDatabase(r -> r.name(name));
            return TaskResult.success()
                    .value("exists", response.database() != null);
        } catch (EntityNotFoundException e) {
            return TaskResult.success()
                    .value("exists", false);
        } catch (GlueException e) {
            log.error("❌ Failed to check glue database '{}': {}", name, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }
}
