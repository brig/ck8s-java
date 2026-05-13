package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

import javax.inject.Inject;
import java.util.Set;

import static ca.vanzyl.ck8s.aws.cognito.actions.UpsertUserPoolDomainAction.getDomain;

public class VerifyUserPoolDomainAction extends CognitoTaskAction<CognitoTaskParams.CreateUserPoolDomainParams> {

    private static final Logger log = LoggerFactory.getLogger(VerifyUserPoolDomainAction.class);

    @Inject
    public VerifyUserPoolDomainAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.VERIFY_USER_POOL_DOMAIN;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateUserPoolDomainParams input) throws Exception {
        var poolId = input.poolId();
        var domain = input.domain();

        try (var client = createClient(input)) {
            var existingDomain = getDomain(client, poolId, domain);
            if (existingDomain == null) {
                log.info("❌ Domain '{}' does not exists", domain);
                return TaskResult.fail("Domain does not exist");
            }
            log.info("✅ Domain '{}' already exists", domain);
            return TaskResult.success();
        } catch (CognitoIdentityProviderException e) {
            log.error("❌ Failed to verify domain '{}': {}", domain, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }
}
