package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.asserts.json.JsonComparatorV2;
import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.IamException;
import software.amazon.awssdk.services.iam.model.NoSuchEntityException;
import software.amazon.awssdk.services.iam.model.PutRolePolicyRequest;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.PutRolePolicyParams;

public class PutPolicyOrVerifyAction extends IamTaskAction<PutRolePolicyParams> {

    private final static Logger log = LoggerFactory.getLogger(PutPolicyOrVerifyAction.class);

    @Inject
    public PutPolicyOrVerifyAction(IamClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.PUT_ROLE_POLICY_OR_VERIFY;
    }

    @Override
    public TaskResult execute(Context context, PutRolePolicyParams input) throws Exception {
        var roleName = input.roleName();
        var policyName = input.policyName();
        var policyDocument = input.policyDocument();

        try (var client = createClient(input)) {
            var currentDocument = inlinePolicyDocument(client, roleName, policyName);
            if (currentDocument != null) {
                log.info("Inline policy '{}' for role '{}' exists. Verifying it...", policyName, roleName);
                var valid = verifyPolicyDocument(currentDocument, policyDocument);
                if (!valid) {
                    return TaskResult.fail("Inline policy document differs from current policy");
                }
                log.info("✅ Inline policy valid");
            } else {
                log.info("Inline policy '{}' for role '{}' does not exists. Creating it...", policyName, roleName);

                client.putRolePolicy(PutRolePolicyRequest.builder()
                        .roleName(roleName)
                        .policyName(policyName)
                        .policyDocument(policyDocument)
                        .build());

                log.info("✅ Successfully putted inline policy '{}' to role '{}'", policyName, roleName);
            }

            return TaskResult.success();
        } catch (IamException e) {
            log.error("❌ Failed to put policy '{}' to role '{}': {}", policyName, roleName, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }

    public static String inlinePolicyDocument(IamClient client, String roleName, String policyName) {
        try {
            var document = client.getRolePolicy(r -> r.roleName(roleName)
                            .policyName(policyName))
                    .policyDocument();
            if (document == null) {
                return null;
            }
            return java.net.URLDecoder.decode(document, StandardCharsets.UTF_8);
        } catch (NoSuchEntityException e) {
            return null;
        }
    }

    public static boolean verifyPolicyDocument(String currentPolicy, String newPolicy) {
        var compareResult = new JsonComparatorV2().compare(newPolicy, currentPolicy);

        if (!compareResult.success()) {
            log.error("❌ Inline policy document differs from current policy. This requires manual verification");
            log.info("Current document:\n{}", currentPolicy);
            log.info("New document:\n{}", newPolicy);
            log.info("Diff: {}", compareResult.message());
            return false;
        }

        log.info("✅ Policy document unchanged");

        return true;
    }
}
