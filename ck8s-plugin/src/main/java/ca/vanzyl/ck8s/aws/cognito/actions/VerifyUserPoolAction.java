package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.AwsTaskUtils;
import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SchemaAttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolType;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ca.vanzyl.ck8s.utils.VerifyUtils.*;

public class VerifyUserPoolAction extends CognitoTaskAction<CognitoTaskParams.CreateUserPoolParams> {

    private static final Logger log = LoggerFactory.getLogger(VerifyUserPoolAction.class);


    @Inject
    public VerifyUserPoolAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.VERIFY_USER_POOL;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateUserPoolParams input) throws Exception {
        var poolName = input.poolName();

        try (var client = createClient(input)) {
            var poolId = UpsertUserPoolAction.finPoolIdByName(client, poolName);
            if (poolId == null) {
                log.error("❌ Cognito User Pool '{}' does not exists", poolName);
                return TaskResult.fail("Cognito User pool not found");
            }

            log.info("User pool '{}' exists. Verifying it...", poolName);

            var pool = client.describeUserPool(r -> r.userPoolId(poolId)).userPool();
            return diffUserPool(pool, UserPoolType.builder()
                    .id(poolId)
                    .name(poolName)
                    .usernameConfiguration(input.usernameConfiguration())
                    .usernameAttributes(input.usernameAttributes())
                    .policies(input.policy())
                    .adminCreateUserConfig(input.adminCreateUserConfig())
                    .emailConfiguration(input.emailConfiguration())
                    .schemaAttributes(input.schema())
                    .userPoolTags(input.tags())
                    .build());
        }
    }

    public static TaskResult diffUserPool(UserPoolType existingPool, UserPoolType newPool) {
        var valid = true;

        verifyAttribute("Username configuration (ignoring)", existingPool.usernameConfiguration(), newPool.usernameConfiguration());
        valid &= verifyPartialMapMatch("Admin create user config", existingPool.adminCreateUserConfig(), newPool.adminCreateUserConfig());
        valid &= verifyAttribute("Policies", existingPool.policies(), newPool.policies());
        valid &= verifyAttribute("Email configuration", existingPool.emailConfiguration(), newPool.emailConfiguration());
        valid &= verifyAttribute("Username attributes", existingPool.usernameAttributes(), newPool.usernameAttributes());

        valid &= verifyPartialListMatch("Schema attributes", toMap(existingPool.schemaAttributes()), toMap(newPool.schemaAttributes().stream()
                .map(a -> a.toBuilder()
                        .name("custom:" + a.name()).build())
                .toList()),
                true); // TODO: ignore for now
        valid &= verifyPartialMapMatch("Tags", existingPool.userPoolTags(), newPool.userPoolTags());

        if (!valid) {
            log.info("❌ User pool '{}' changed", existingPool.name());

            var attrs = List.of("usernameConfiguration", "adminCreateUserConfig", "policies", "emailConfiguration", "usernameAttributes", "schemaAttributes");
            dumpDiff("user pool", existingPool, newPool, attrs);

            return TaskResult.fail("User pool changed");
        }

        log.info("✅ User pool '{}' unchanged", newPool.name());

        return TaskResult.success()
                .value("id", existingPool.id());
    }

    private static List<Map<String, Object>> toMap(List<SchemaAttributeType> schemaAttributeTypes) {
        return AwsTaskUtils.serializeList(schemaAttributeTypes);
    }
}
