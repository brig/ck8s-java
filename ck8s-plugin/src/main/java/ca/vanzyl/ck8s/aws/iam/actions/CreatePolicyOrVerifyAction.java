package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.asserts.json.JsonComparatorV2;
import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.CreatePolicyRequest;
import software.amazon.awssdk.services.iam.model.GetPolicyVersionRequest;
import software.amazon.awssdk.services.iam.model.IamException;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.CreatePolicyParams;
import static ca.vanzyl.ck8s.aws.iam.actions.CreatePolicyAction.dumpInput;

public class CreatePolicyOrVerifyAction extends IamTaskAction<CreatePolicyParams> {

    private final static Logger log = LoggerFactory.getLogger(CreatePolicyOrVerifyAction.class);

    @Inject
    public CreatePolicyOrVerifyAction(IamClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.CREATE_POLICY_OR_VERIFY;
    }

    @Override
    public TaskResult execute(Context context, CreatePolicyParams input) throws Exception {
        var policyName = input.policyName();
        var policyArn = input.policyArn();
        var policyDocument = input.policyDocument();

        try (var client = createClient(input)) {
            var policy = CreatePolicyAction.getPolicy(client, policyArn);
            if (policy != null) {
                log.info("Policy '{}' exists. Verifying it...", policyName);

                var currentPolicyDocument = policyDocument(client, policyArn, policy.defaultVersionId());
                return diffPolicy(currentPolicyDocument, policyDocument);
            } else {
                log.info("Policy '{}' does not exists. Creating it...", policyName);

                dumpInput(input);

                var newPolicy = client.createPolicy(CreatePolicyRequest.builder()
                        .policyName(policyName)
                        .policyDocument(policyDocument)
                        .build()).policy();

                log.info("✅ Successfully created policy '{}' (arn: '{}')", policyName, newPolicy.arn());
            }

            return TaskResult.success();
        } catch (IamException e) {
            log.error("❌ Failed to create policy '{}': {}", policyName, e.awsErrorDetails().errorMessage());
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

    public static TaskResult diffPolicy(String currentPolicy, String newPolicy) {
        var compareResult = new JsonComparatorV2().compare(newPolicy, currentPolicy);

        if (!compareResult.success()) {
            log.error("❌ Policy document differs from current policy. This requires manual verification");
            log.info("Current policy document:\n{}", currentPolicy);
            log.info("New policy document:\n{}", newPolicy);
            log.info("Diff: {}", compareResult.message());
            return TaskResult.fail("Policy document differs from current policy");
        }

        log.info("✅ Policy document unchanged");
        return TaskResult.success();
    }
}
