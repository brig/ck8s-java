package ca.vanzyl.ck8s.aws;

import com.walmartlabs.concord.client2.ApiException;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static ca.vanzyl.ck8s.aws.AwsTaskUtils.assertRegion;

@Named("ck8sIamPolicy")
@DryRunReady
public class IamPolicyTask implements Task {

    private final static Logger log = LoggerFactory.getLogger(IamPolicyTask.class);

    private final Context context;
    private final LockService lockService;
    private final CredentialsProvider credentialsProvider;
    private final boolean dryRunMode;

    @Inject
    public IamPolicyTask(Context context, LockService lockService, CredentialsProvider credentialsProvider) {
        this.context = context;
        this.lockService = lockService;
        this.credentialsProvider = credentialsProvider;
        this.dryRunMode = context.processConfiguration().dryRun();
    }

    @Override
    public TaskResult execute(Variables input) throws Exception {
        var action = input.assertString("action");
        if ("remove-resources".equals(action)) {
            return removeStatementResourcesFromPolicy(input);
        } else if ("add-resources".equals(action)) {
            return addStatementResourcesFromPolicy(input);
        }

        throw new IllegalArgumentException("Unsupported action: " + action);
    }

    private TaskResult addStatementResourcesFromPolicy(Variables input) {
        String roleName = input.assertString("role");
        String policyName = input.assertString("policy");
        PolicyContent policyToAdd = getPolicyFromTemplate(input);
        boolean skipLock = input.getBoolean("skip-lock", false);

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping adding resources to policy '{}' for role '{}'", policyName, roleName);
            return TaskResult.success();
        }

        log.info("Adding resources to policy '{}' for role '{}'", policyName, roleName);

        try (var client = IamClient.builder()
                .region(assertRegion(input))
                .credentialsProvider(credentialsProvider.get(input))
                .build();
             var lock = lockPolicyIfNeed(policyName, input)) {

            Policy policy = getPolicy(client, roleName, policyName);
            if (policy == null) {
                createPolicy(client, roleName, policyName, policyToAdd, input.getMap("tags", Map.of()));
                return TaskResult.success();
            }

            log.info("Policy '{}' exists -> updating", policyName);

            PolicyContent policyContent = getPolicyContent(client, policy.arn(), policy.defaultVersionId());
            if (policyContent == null) {
                log.info("Policy content for '{}' for role '{}' is empty", policyName, roleName);
                updatePolicy(client, policy, policyToAdd);
                return TaskResult.success();
            }

            boolean policyUpdated = PolicyContent.add(policyContent, policyToAdd);
            if (!policyUpdated) {
                log.info("No changes in policy '{}'", policyName);
                return TaskResult.success();
            }

            updatePolicy(client, policy, policyContent);

            return TaskResult.success();
        } catch (Exception e) {
            log.error("Error adding resources to policy '{}' for role '{}'", policyName, roleName, e);
            return TaskResult.fail(e);
        }
    }

    private AutoCloseable lockPolicyIfNeed(String policyName, Variables input) throws ApiException {
        if (input.getBoolean("lockPolicy", false)) {
            return lockService.lock(policyName);
        }

        return () -> {
            // do nothing
        };
    }

    private TaskResult removeStatementResourcesFromPolicy(Variables input) {
        String roleName = input.assertString("role");
        String policyName = input.assertString("policy");
        PolicyContent policyToRemove = getPolicyFromTemplate(input);

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping removing resources from policy '{}' role '{}'", policyName, roleName);
            return TaskResult.success();
        }

        log.info("Removing resources from policy '{}' for role '{}'", policyName, roleName);

        try (var client = IamClient.builder()
                .region(assertRegion(input))
                .credentialsProvider(credentialsProvider.get(input))
                .build();

             var lock = lockService.lock(policyName)) {

            Policy policy = getPolicy(client, roleName, policyName);
            if (policy == null) {
                log.info("Policy '{}' for role '{}' not found", policyName, roleName);
                return TaskResult.success();
            }

            log.info("Policy '{}' for role '{}' -> arn: '{}', version: '{}'", policyName, roleName, policy.arn(), policy.defaultVersionId());

            PolicyContent policyContent = getPolicyContent(client, policy.arn(), policy.defaultVersionId());
            if (policyContent == null) {
                log.info("Policy content for '{}' for role '{}' is empty", policyName, roleName);
                return TaskResult.success();
            }

            boolean policyUpdated = PolicyContent.remove(policyContent, policyToRemove);
            if (!policyUpdated) {
                log.info("No changes in policy '{}'", policyName);
                return TaskResult.success();
            }

            if (policyContent.statements().isEmpty()) {
                log.info("All statements removed from policy '{}' => removing policy", policy);
                deletePolicy(client, roleName, policy.arn());
            } else {
                updatePolicy(client, policy, policyContent);
            }

            return TaskResult.success();
        } catch (Exception e) {
            log.error("Error removing resources from policy '{}' for role '{}'", policyName, roleName, e);
            return TaskResult.fail(e);
        }
    }

    private static PolicyContent getPolicyFromTemplate(Variables input) {
        String templatePath = input.assertString("template");
        Map<String, String> args = input.getMap("templateArgs", Map.of());
        try {
            return PolicyContent.fromTemplate(Path.of(templatePath), args);
        } catch (IOException e) {
            log.error("Can't load policy template from '{}'", templatePath, e);
            throw new RuntimeException(e);
        }
    }

    private static Policy getPolicy(IamClient client, String roleName, String policyName) {
        try {
            var result = client.listAttachedRolePolicies(
                    ListAttachedRolePoliciesRequest.builder()
                            .roleName(roleName)
                            .build());

            AttachedPolicy attachedPolicy = result.attachedPolicies().stream()
                    .filter(p -> policyName.equals(p.policyName()))
                    .findFirst()
                    .orElse(null);

            if (attachedPolicy == null) {
                return null;
            }

            var response = client.getPolicy(
                    GetPolicyRequest.builder()
                            .policyArn(attachedPolicy.policyArn())
                            .build());
            return response.policy();
        } catch (IamException e) {
            log.error("getPolicy ['{}', '{}'] -> error", roleName, policyName);
            throw e;
        }
    }

    private static void createPolicy(IamClient client, String roleName, String policyName, PolicyContent policyContent, Map<String, String> tags) {
        try {
            List<Tag> tagsSpec = tags.entrySet().stream()
                    .map(tag -> Tag.builder()
                            .key(tag.getKey())
                            .value(tag.getValue())
                            .build())
                    .toList();

            CreatePolicyRequest createPolicyRequest = CreatePolicyRequest.builder()
                    .policyName(policyName)
                    .policyDocument(PolicyContent.toString(policyContent))
                    .tags(tagsSpec)
                    .build();

            var createPolicyResponse = client.createPolicy(createPolicyRequest);

            String policyArn = createPolicyResponse.policy().arn();
            AttachRolePolicyRequest attachRolePolicyRequest = AttachRolePolicyRequest.builder()
                    .roleName(roleName)
                    .policyArn(policyArn)
                    .build();

            client.attachRolePolicy(attachRolePolicyRequest);

            log.info("New policy '{}' attached to role '{}'", policyName, roleName);
        } catch (Exception e) {
            log.error("createPolicy ['{}', '{}']", roleName, policyName);
            throw new RuntimeException(e);
        }
    }

    private static void cleanupPolicyVersions(IamClient client, String policyArn) {
        log.info("Cleanup policy with ARN '{}' versions", policyArn);

        var response = client.listPolicyVersions(ListPolicyVersionsRequest.builder().policyArn(policyArn).build());
        for (PolicyVersion pv : response.versions()) {
            if (!pv.isDefaultVersion()) {
                try {
                    client.deletePolicyVersion(DeletePolicyVersionRequest.builder().policyArn(policyArn).versionId(pv.versionId()).build());
                } catch (NoSuchEntityException e) {
                    log.info("Policy version '{}' -> not found", pv.versionId());
                }
            }
        }
    }

    private static void deletePolicy(IamClient client, String roleName, String policyArn) {
        try {
            cleanupPolicyVersions(client, policyArn);

            log.info("Detaching role policy with ARN '{}'", policyArn);
            client.detachRolePolicy(DetachRolePolicyRequest.builder().policyArn(policyArn).build());

            log.info("Deleting policy with ARN '{}'", policyArn);
            client.deletePolicy(DeletePolicyRequest.builder().policyArn(policyArn).build());
        } catch (IamException e) {
            log.error("Error deleting policy '{}' with ARN '{}' ", roleName, policyArn);
            throw e;
        }
    }

    private void updatePolicy(IamClient client, Policy policy, PolicyContent policyContent) {
        try {
            cleanupPolicyVersions(client, policy.arn());

            var result = client.createPolicyVersion(CreatePolicyVersionRequest.builder()
                    .policyArn(policy.arn())
                    .policyDocument(PolicyContent.toString(policyContent))
                    .setAsDefault(true)
                    .build());

            log.info("Policy '{}' updated: {}", policy.policyName(), result.policyVersion());
        } catch (IamException e) {
            log.error("Error updating policy '{}'", policy.policyName());
            throw e;
        }
    }

    private PolicyContent getPolicyContent(IamClient client, String policyArn, String versionId) {
        try {
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

            String policyDocument = java.net.URLDecoder.decode(document, StandardCharsets.UTF_8);
            if (context.processConfiguration().debug()) {
                log.info("Policy:\n{}", policyDocument);
            }
            return PolicyContent.fromString(policyDocument);
        } catch (Exception e) {
            log.error("getPolicyContent ['{}', '{}'] -> error", policyArn, versionId);
            throw new RuntimeException(e);
        }
    }
}
