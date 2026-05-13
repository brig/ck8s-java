package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.asserts.json.JsonComparatorV2;
import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.model.IamException;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.CreateRoleParams;
import static ca.vanzyl.ck8s.aws.iam.actions.CreateRoleAction.createRole;
import static ca.vanzyl.ck8s.aws.iam.actions.CreateRoleAction.dumpInput;

public class CreateRoleOrVerifyAction extends IamTaskAction<CreateRoleParams> {

    private final static Logger log = LoggerFactory.getLogger(CreateRoleOrVerifyAction.class);

    @Inject
    public CreateRoleOrVerifyAction(IamClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.CREATE_ROLE_OR_VERIFY;
    }

    @Override
    public TaskResult execute(Context context, CreateRoleParams input) throws Exception {
        var roleName = input.roleName();
        var trustPolicy = input.trustPolicy();
        var tags = input.tags();

        dumpInput(input);

        try (var client = createClient(input)) {
            var role = GetRoleAction.getRole(client, roleName);
            if (role != null) {
                log.info("Role '{}' exists. Verifying it...", roleName);

                var currentTrustPolicy = java.net.URLDecoder.decode(role.assumeRolePolicyDocument(), StandardCharsets.UTF_8);
                var valid = verifyRoleTrustPolicy(currentTrustPolicy, trustPolicy);
                if (!valid) {
                    return TaskResult.fail("Trust policy document differs from current policy");
                }
                log.info("✅ Role valid");
            } else {
                log.info("Role '{}' does not exists. Creating it...", roleName);
                createRole(client, roleName, trustPolicy, tags);
            }

            return TaskResult.success();
        } catch (IamException e) {
            log.error("❌ Failed to create role '{}': {}", roleName, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        } catch (RuntimeException e) {
            return TaskResult.fail(e);
        }
    }

    public static boolean verifyRoleTrustPolicy(String currentPolicy, String newPolicy) {
        var compareResult = new JsonComparatorV2().compare(newPolicy, currentPolicy);

        if (!compareResult.success()) {
            log.error("❌ Trust policy document differs from current policy. This requires manual verification");
            log.info("Current document:\n{}", currentPolicy);
            log.info("New document:\n{}", newPolicy);
            log.info("Diff: {}", compareResult.message());
            return false;
        }

        log.info("✅ Policy document unchanged");

        return true;
    }
}
