package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.GetPolicyVersionRequest;
import software.amazon.awssdk.services.iam.model.IamException;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.CreatePolicyParams;
import static ca.vanzyl.ck8s.utils.VerifyUtils.verifyJsonAttribute;

public class VerifyPolicyAction extends IamTaskAction<CreatePolicyParams> {

    private final static Logger log = LoggerFactory.getLogger(VerifyPolicyAction.class);

    @Inject
    public VerifyPolicyAction(IamClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.VERIFY_POLICY;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, CreatePolicyParams input) throws Exception {
        var policyName = input.policyName();
        var policyArn = input.policyArn();
        var policyDocument = input.policyDocument();

        try (var client = createClient(input)) {
            var policy = CreatePolicyAction.getPolicy(client, policyArn);
            if (policy == null) {
                log.error("❌ Managed policy '{}' for does not exists", policyName);
                return TaskResult.fail("Managed policy does not exist");
            }

            var currentPolicyDocument = policyDocument(client, policyArn, policy.defaultVersionId());

            var valid = verifyPolicy(new Policy(currentPolicyDocument), new Policy(policyDocument));
            if (!valid) {
                log.info("❌ Policy '{}' changed", policyName);
                return TaskResult.fail("Policy changed");
            }

            log.info("✅ Policy '{}' valid", policyName);

            return TaskResult.success();
        } catch (IamException e) {
            log.error("❌ Failed to verify policy '{}': {}", policyName, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }

    public static String policyDocument(IamClient client, String policyArn, String versionId) {
        var response = client.getPolicyVersion(GetPolicyVersionRequest.builder()
                .policyArn(policyArn)
                .versionId(versionId)
                .build());

        if (response.policyVersion() == null) {
            return null;
        }

        String document = response.policyVersion().document();
        if (document == null || document.isBlank()) {
            return null;
        }

        return java.net.URLDecoder.decode(document, StandardCharsets.UTF_8);
    }

    private boolean verifyPolicy(Policy existingPolicy, Policy newPolicy) {
        return verifyJsonAttribute("Policy document", existingPolicy.document(), newPolicy.document());
    }

    public record Policy (String document) {
    }
}
