package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.google.inject.Inject;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientDescription;

import java.util.Map;
import java.util.Set;

public class FindUserPoolClientAction extends CognitoTaskAction<CognitoTaskParams.FindUserPoolClientParams> {

    private static final Logger log = LoggerFactory.getLogger(FindUserPoolClientAction.class);

    private static final int MAX_RESULTS = 50;

    @Inject
    public FindUserPoolClientAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.FIND_USER_POOL_CLIENT;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.FindUserPoolClientParams input) throws Exception {
        var poolId = input.poolId();
        var clientName = input.clientName();
        try (var client = createClient(input)) {
            var existingClient = findClientByName(client, poolId, clientName);
            if (existingClient == null) {
                return TaskResult.success();
            }
            return TaskResult.success()
                    .value("client", Map.of("id", existingClient.clientId(), "name", existingClient.clientName()));
        } catch (CognitoIdentityProviderException e) {
            log.error("❌ Failed to find client with name '{}' in user pool '{}': {}", clientName, poolId, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }

    public static UserPoolClientDescription findClientByName(CognitoIdentityProviderClient client, String poolId, String clientName) {
        var clients = client.listUserPoolClientsPaginator(
                        r -> r.userPoolId(poolId)
                                .maxResults(MAX_RESULTS)).stream()
                .flatMap(response -> response.userPoolClients().stream())
                .filter(u -> clientName.equals(u.clientName()))
                .toList();

        if (clients.size() > 1) {
            log.error("❌ Multiple clients ({}) found for name '{}' in pool '{}'", clients.size(), clientName, poolId);
            log.error("Clients: {}", clients);
            throw new RuntimeException("Multiple user pools clients found for name: " + clientName);
        } else if (clients.isEmpty()) {
            log.info("No user pool client found for name '{}' in pool '{}'", clientName, poolId);
            return null;
        }

        return clients.get(0);
    }
}
