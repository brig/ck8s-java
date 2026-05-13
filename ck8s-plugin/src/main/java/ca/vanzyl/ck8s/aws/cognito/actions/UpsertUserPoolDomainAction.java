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
import software.amazon.awssdk.services.cognitoidentityprovider.model.DomainDescriptionType;

import javax.inject.Inject;

public class UpsertUserPoolDomainAction extends CognitoTaskAction<CognitoTaskParams.CreateUserPoolDomainParams> {

    private static final Logger log = LoggerFactory.getLogger(UpsertUserPoolDomainAction.class);

    @Inject
    public UpsertUserPoolDomainAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.UPSERT_USER_POOL_DOMAIN;
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateUserPoolDomainParams input) throws Exception {
        var poolId = input.poolId();
        var domain = input.domain();

        try (var client = createClient(input)) {
            var existingDomain = getDomain(client, poolId, domain);
            if (existingDomain == null) {
                log.info("Domain '{}' does not exists. Creating it...", domain);

                client.createUserPoolDomain(r -> r.userPoolId(poolId)
                        .domain(domain)
                        .build());

                log.info("✅ Domain '{}' created for pool: '{}'", domain, poolId);
            } else {
                log.info("✅ Domain '{}' already exists. Do nothing...", domain);
            }
            return TaskResult.success();
        } catch (CognitoIdentityProviderException e) {
            log.error("❌ Failed to upsert domain '{}': {}", domain, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }

    public static DomainDescriptionType getDomain(CognitoIdentityProviderClient client, String poolId, String domain) {
        var existingDomain = client.describeUserPoolDomain(r -> r.domain(domain)).domainDescription();
        if (existingDomain.userPoolId() == null) {
            return null;
        }
        if (!poolId.equals(existingDomain.userPoolId())) {
            throw new RuntimeException("Domain '" + domain + "' belongs to user pool '" + existingDomain.userPoolId() + "', but expected: " + poolId);
        }
        return existingDomain;
    }
}
