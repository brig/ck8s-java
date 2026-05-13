package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ProviderDescription;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceNotFoundException;

import javax.inject.Inject;
import java.util.List;

public class UpsertIdentityProviderAction extends CognitoTaskAction<CognitoTaskParams.CreateIdentityProviderParams> {

    private static final Logger log = LoggerFactory.getLogger(UpsertIdentityProviderAction.class);

    private static final int MAX_RESULTS = 50;

    @Inject
    public UpsertIdentityProviderAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.UPSERT_IDENTITY_PROVIDER;
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateIdentityProviderParams input) throws Exception {
        var poolId = input.poolId();
        var providerName = input.providerName();
        var providerType = input.providerType();
        var providerDetails = input.providerDetails();
        var attributeMapping = input.attributeMapping();

        try (var client = createClient(input)) {
            var existingProvider = findIdentityProviderByName(client, poolId, providerName);
            if (existingProvider != null) {
                log.info("Identity provider '{}' exists in pool '{}'. Updating it...", providerName, poolId);

                client.updateIdentityProvider(r -> r.userPoolId(poolId)
                        .providerName(providerName)
                        .providerDetails(providerDetails)
                        .attributeMapping(attributeMapping));

                log.info("✅ Identity provider '{}' in pool '{}' updated", providerName, poolId);

                return TaskResult.success();
            }

            log.info("Identity provider '{}' in pool '{}' does not exists. Creating it...", providerName, poolId);

            client.createIdentityProvider(r -> r.userPoolId(poolId)
                    .providerName(providerName)
                    .providerType(providerType)
                    .providerDetails(providerDetails)
                    .attributeMapping(attributeMapping));

            log.info("✅ Identity provider '{}' in pool '{}' created", providerName, poolId);

            return TaskResult.success();
        } catch (CognitoIdentityProviderException e) {
            System.err.println("❌ Failed to create identity provider: " + e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }

    public static ProviderDescription findIdentityProviderByName(CognitoIdentityProviderClient client, String userPoolId, String providerName) {
        List<ProviderDescription> providers;
        try {
            providers = client.listIdentityProvidersPaginator(r -> r.maxResults(MAX_RESULTS)
                            .userPoolId(userPoolId)).stream()
                    .flatMap(r -> r.providers().stream())
                    .filter(i -> providerName.equals(i.providerName()))
                    .toList();
        } catch (ResourceNotFoundException e) {
            return null;
        }

        if (providers.size() > 1) {
            log.error("❌ Multiple identity providers ({}) found for name '{}' in pool '{}'", providers.size(), providerName, userPoolId);
            log.error("Providers: {}", providers);
            throw new RuntimeException("Multiple identity providers found for name: " + providerName);
        } else if (providers.isEmpty()) {
            log.info("No identity providers found for name '{}'", providerName);
            return null;
        }
        return providers.get(0);
    }
}
