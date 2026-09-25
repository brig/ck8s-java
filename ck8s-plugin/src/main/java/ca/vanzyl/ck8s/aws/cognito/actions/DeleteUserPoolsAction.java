package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.google.inject.Inject;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeleteUserPoolDomainRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeleteUserPoolRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolDomainRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DeleteUserPoolsAction extends CognitoTaskAction<CognitoTaskParams.DeleteUserPoolsParams> {

    private static final Logger log = LoggerFactory.getLogger(DeleteUserPoolsAction.class);

    private static final long POLL_INTERVAL = TimeUnit.SECONDS.toMillis(5);
    private static final Duration DOMAIN_DELETE_TIMEOUT = Duration.ofMinutes(5);

    public static void deleteUserPools(CognitoIdentityProviderClient client, List<String> ids) {
        for (var id : ids) {
            var poolResponse = client.describeUserPool(DescribeUserPoolRequest.builder()
                    .userPoolId(id)
                    .build());
            if (poolResponse != null && poolResponse.userPool().domain() != null) {
                var domain = poolResponse.userPool().domain();

                log.info("Deleting user pool '{}' domain: '{}'", id, domain);

                client.deleteUserPoolDomain(DeleteUserPoolDomainRequest.builder()
                        .userPoolId(id)
                        .domain(domain)
                        .build());

                awaitDomainDeleted(client, domain);
            }
            log.info("Deleting user pool '{}'", id);
            client.deleteUserPool(DeleteUserPoolRequest.builder().userPoolId(id).build());
        }
    }

    /**
     * DeleteUserPoolDomain returns as soon as the domain enters DELETING, and until it is
     * actually gone DeleteUserPool fails with "It has a domain configured that should be
     * deleted first". The SDK ships no waiter for Cognito, so poll DescribeUserPoolDomain:
     * a deleted domain comes back with an empty description.
     */
    private static void awaitDomainDeleted(CognitoIdentityProviderClient client, String domain) {
        log.info("Waiting for domain '{}' to be deleted...", domain);

        var deadline = Instant.now().plus(DOMAIN_DELETE_TIMEOUT);

        while (!Thread.currentThread().isInterrupted()) {
            var description = client.describeUserPoolDomain(DescribeUserPoolDomainRequest.builder()
                            .domain(domain)
                            .build())
                    .domainDescription();

            if (description == null || description.domain() == null) {
                log.info("✅ Domain '{}' deleted", domain);
                return;
            }

            if (Instant.now().isAfter(deadline)) {
                throw new UserDefinedException(String.format(
                        "Timed out after %s waiting for the user pool domain '%s' to be deleted, last status: %s",
                        DOMAIN_DELETE_TIMEOUT, domain, description.status()));
            }

            log.info("Domain '{}' status: {}", domain, description.status());

            sleep(POLL_INTERVAL);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Inject
    public DeleteUserPoolsAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.DELETE_USER_POOLS;
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.DeleteUserPoolsParams input) throws Exception {
        var ids = input.ids();

        if (ids.isEmpty()) {
            log.info("No user pools provided, skipping delete");
            return TaskResult.success();
        }

        try (var client = createClient(input)) {

            deleteUserPools(client, ids);

            return TaskResult.success();
        } catch (CognitoIdentityProviderException e) {
            log.error("❌ Failed to delete user pools: {}", e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }
}
