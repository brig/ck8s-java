package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceServerType;

import javax.inject.Inject;
import java.util.Set;

import static ca.vanzyl.ck8s.utils.VerifyUtils.verifyAttribute;

public class VerifyResourceServerAction extends CognitoTaskAction<CognitoTaskParams.CreateResourceServerParams> {

    private static final Logger log = LoggerFactory.getLogger(VerifyResourceServerAction.class);

    @Inject
    public VerifyResourceServerAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.VERIFY_RESOURCE_SERVER;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateResourceServerParams input) throws Exception {
        var poolId = input.poolId();
        var name = input.name();
        var identifier = input.identifier();
        var scopes = input.scopes();

        try (var client = createClient(input)) {
            var existingServer = UpsertResourceServerAction.getResourceServer(client, poolId, identifier);
            if (existingServer == null) {
                log.error("❌ Resource server '{}' does not exists in pool '{}'", identifier, poolId);
                return TaskResult.fail("Resource server not found");
            }

            log.info("Resource server '{}' exists in pool '{}'. Verifying it...", identifier, poolId);

            return diffResourceServer(existingServer, ResourceServerType.builder()
                    .userPoolId(poolId)
                    .identifier(identifier)
                    .name(name)
                    .scopes(scopes)
                    .build());
        }
    }

    public static TaskResult diffResourceServer(ResourceServerType existingServer, ResourceServerType newServer) {
        var valid = true;

        valid &= verifyAttribute("Name", existingServer.name(), newServer.name());
        valid &= verifyAttribute("Scopes", existingServer.scopes(), newServer.scopes());

        if (!valid) {
            log.info("❌ Resource server '{}' changed", existingServer.identifier());

            return TaskResult.fail("Resource server changed");
        }

        log.info("✅ Resource server '{}' unchanged", existingServer.identifier());

        return TaskResult.success();
    }
}
