package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import ca.vanzyl.ck8s.aws.iam.IamTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;

import javax.inject.Inject;
import java.util.HashSet;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.CreatePolicyParams;

public class CreatePolicyAction extends IamTaskAction<CreatePolicyParams> {

    private final static Logger log = LoggerFactory.getLogger(CreatePolicyAction.class);

    @Inject
    public CreatePolicyAction(IamClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.CREATE_POLICY;
    }

    @Override
    public TaskResult execute(Context context, CreatePolicyParams input) throws Exception {
        var policyName = input.policyName();
        var policyArn = input.policyArn();
        var policyDocument = input.policyDocument();

        dumpInput(input);

        try (var client = createClient(input)) {
            if (getPolicy(client, policyArn) != null) {
                log.info("Policy '{}' exists. Updating it...", policyName);

                replaceManagedPolicy(client, policyArn, policyDocument);

                log.info("✅ Successfully updated policy '{}' (arn: '{}')", policyName, policyArn);
            } else {
                log.info("Policy '{}' does not exists. Creating it...", policyName);

                var policy = client.createPolicy(CreatePolicyRequest.builder()
                        .policyName(policyName)
                        .policyDocument(policyDocument)
                        .build()).policy();

                log.info("✅ Successfully created policy '{}' (arn: '{}')", policyName, policy.arn());
            }

            return TaskResult.success();
        } catch (IamException e) {
            log.error("❌ Failed to create policy '{}': {}", policyName, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }

    public static void dumpInput(IamTaskParams.CreatePolicyParams input) {
        if (input.baseParams().debug()) {
            log.info("Policy:\n{}", input.policyDocument());
        }
    }

    public static Policy getPolicy(IamClient client, String policyArn) {
        try {
            return client.getPolicy(GetPolicyRequest.builder()
                    .policyArn(policyArn)
                    .build()).policy();
        } catch (NoSuchEntityException e) {
            return null;
        }
    }

    public static void replaceManagedPolicy(IamClient client, String policyArn, String policyDocument) {
        cleanupPolicyVersions(client, policyArn);
        createNewPolicyVersion(client, policyArn, policyDocument);
    }

    public static void cleanupPolicyVersions(IamClient client, String policyArn) {
        log.info("Cleanup policy versions...");

        ListPolicyVersionsResponse response;
        try {
            response = client.listPolicyVersions(ListPolicyVersionsRequest.builder().policyArn(policyArn).build());
        } catch (NoSuchEntityException e) {
            log.info("Policy '{}' does not exist", policyArn);
            return;
        }

        var deletedVersions = new HashSet<String>();
        for (PolicyVersion pv : response.versions()) {
            if (!pv.isDefaultVersion()) {
                deletedVersions.add(pv.versionId());
                try {
                    client.deletePolicyVersion(DeletePolicyVersionRequest.builder().policyArn(policyArn).versionId(pv.versionId()).build());
                } catch (NoSuchEntityException e) {
                    log.info("Policy version '{}' -> not found, ignoring", pv.versionId());
                }
            }
        }

        log.info("✅ Successfully cleaned up policy '{}' versions: {}", policyArn, deletedVersions);
    }

    private static void createNewPolicyVersion(IamClient client, String policyArn, String policyDocument) {
        var response = client.createPolicyVersion(
                CreatePolicyVersionRequest.builder()
                        .policyArn(policyArn)
                        .policyDocument(policyDocument)
                        .setAsDefault(true)
                        .build());
        log.info("✅ New policy version created: {}", response.policyVersion().versionId());
    }
}
