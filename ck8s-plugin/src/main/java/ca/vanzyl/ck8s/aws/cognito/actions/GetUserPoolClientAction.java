package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.SensitiveData;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceNotFoundException;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

public class GetUserPoolClientAction extends CognitoTaskAction<CognitoTaskParams.GetUserPoolClientParams> {

    private static final Logger log = LoggerFactory.getLogger(GetUserPoolClientAction.class);

    @Inject
    public GetUserPoolClientAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.GET_USER_POOL_CLIENT;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    @SensitiveData(keys = "clientSecret")
    public TaskResult execute(Context context, CognitoTaskParams.GetUserPoolClientParams input) throws Exception {
        var poolId = input.poolId();
        var clientId = input.clientId();

        try (var client = createClient(input)) {
            var poolClient = client.describeUserPoolClient(r -> r.userPoolId(poolId)
                            .clientId(clientId))
                    .userPoolClient();

            return TaskResult.success()
                    .value("client",
                            Map.of("name", poolClient.clientName(),
                                    "clientSecret", poolClient.clientSecret()))
                    .value("clientSecret", poolClient.clientSecret()); // just to mark it as sensitive data
        } catch (ResourceNotFoundException e) {
            log.info("User pool client with id '{}' not found in user pool '{}'", clientId, poolId);
            return TaskResult.success();
        }
    }
}
