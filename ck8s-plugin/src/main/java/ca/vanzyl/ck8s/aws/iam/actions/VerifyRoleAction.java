package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.model.IamException;
import software.amazon.awssdk.services.iam.model.Tag;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.CreateRoleParams;
import static ca.vanzyl.ck8s.aws.iam.actions.CreateRoleAction.dumpInput;
import static ca.vanzyl.ck8s.utils.VerifyUtils.verifyAttribute;
import static ca.vanzyl.ck8s.utils.VerifyUtils.verifyJsonAttribute;

public class VerifyRoleAction extends IamTaskAction<CreateRoleParams> {

    private final static Logger log = LoggerFactory.getLogger(VerifyRoleAction.class);

    @Inject
    public VerifyRoleAction(IamClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.VERIFY_ROLE;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, CreateRoleParams input) throws Exception {
        var roleName = input.roleName();
        var trustPolicy = input.trustPolicy();
        var tags = input.tags();

        dumpInput(input);

        try (var client = createClient(input)) {
            var role = GetRoleAction.getRole(client, roleName);
            if (role == null) {
                log.error("❌ Role '{}' does not exists", roleName);
                return TaskResult.fail("Role does not exist");
            }

            log.info("Role '{}' exists. Verifying it...", roleName);

            var currentTrustPolicy = java.net.URLDecoder.decode(role.assumeRolePolicyDocument(), StandardCharsets.UTF_8);
            var valid = verifyRole(new Role(currentTrustPolicy, role.tags()), new Role(trustPolicy, tags));
            if (!valid) {
                log.info("❌ Role '{}' changed", roleName);
                return TaskResult.fail("Role changed");
            }

            log.info("✅ Role valid");

            return TaskResult.success();
        } catch (IamException e) {
            log.error("❌ Failed to verify role '{}': {}", roleName, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        } catch (RuntimeException e) {
            return TaskResult.fail(e);
        }
    }

    public static boolean verifyRole(Role existingRole, Role newRole) {
        boolean valid = true;

        valid &= verifyAttribute("Role tags", existingRole.tagsAsMap(), newRole.tagsAsMap());
        valid &= verifyJsonAttribute("Trust policy", existingRole.trustPolicy(), newRole.trustPolicy());

        return valid;
    }

    public record Role (String trustPolicy, List<Tag> tags) {
        public Map<String, String> tagsAsMap() {
            return tags.stream()
                    .collect(Collectors.toMap(Tag::key, Tag::value));
        }
    }
}
