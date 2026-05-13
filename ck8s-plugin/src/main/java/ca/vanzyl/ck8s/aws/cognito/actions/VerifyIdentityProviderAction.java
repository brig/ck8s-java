package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.model.IdentityProviderType;
import software.amazon.awssdk.services.glue.model.EntityNotFoundException;

import javax.inject.Inject;
import java.util.Set;

import static ca.vanzyl.ck8s.utils.VerifyUtils.verifyAttribute;
import static ca.vanzyl.ck8s.utils.VerifyUtils.verifyPartialMapMatch;

public class VerifyIdentityProviderAction extends CognitoTaskAction<CognitoTaskParams.CreateIdentityProviderParams> {

    private static final Logger log = LoggerFactory.getLogger(VerifyIdentityProviderAction.class);

    @Inject
    public VerifyIdentityProviderAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.VERIFY_IDENTITY_PROVIDER;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateIdentityProviderParams input) throws Exception {
        var poolId = input.poolId();
        var providerName = input.providerName();
        var providerType = input.providerType();
        var providerDetails = input.providerDetails();
        var attributeMapping = input.attributeMapping();

        try (var client = createClient(input)) {
            var existingProvider = client.describeIdentityProvider(r -> r.userPoolId(poolId)
                    .providerName(providerName));

            log.info("Identity provider '{}' in pool '{}' exists. Verifying...", providerName, poolId);

            return verifyIdentityProvider(existingProvider.identityProvider(),
                    IdentityProviderType.builder()
                            .providerName(providerName)
                            .providerType(providerType)
                            .providerDetails(providerDetails)
                            .attributeMapping(attributeMapping)
                            .build());
        } catch (EntityNotFoundException e) {
            log.error("❌ Cognito Identity provider '{}' for pool '{}' does not exists", providerName, poolId);
            return TaskResult.fail("Cognito Identity provider not found");
        }
    }

    private TaskResult verifyIdentityProvider(IdentityProviderType existingProvider,
                                              IdentityProviderType newProvider) {
        var valid = true;

        valid &= verifyAttribute("Provider type", existingProvider.providerType(), newProvider.providerType());
        valid &= verifyPartialMapMatch("Provider details", existingProvider.providerDetails(), newProvider.providerDetails());
        valid &= verifyPartialMapMatch("Provider attribute mapping", existingProvider.attributeMapping(), newProvider.attributeMapping());

        if (!valid) {
            log.info("❌ Identity Provider '{}' changed", newProvider.providerName());
            return TaskResult.fail("Identity Provider changed");
        }

        log.info("✅ Identity Provider '{}' valid", newProvider.providerName());

        return TaskResult.success();
    }
}
