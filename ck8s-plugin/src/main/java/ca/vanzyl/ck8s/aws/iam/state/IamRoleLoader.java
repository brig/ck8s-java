package ca.vanzyl.ck8s.aws.iam.state;

import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.actions.AttachPolicyAction;
import ca.vanzyl.ck8s.aws.iam.actions.GetRoleAction;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.ListRolePoliciesRequest;
import software.amazon.awssdk.services.iam.model.Tag;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IamRoleLoader extends AbstractIamEntityLoader<IamRoleKey, IamRole> {

    public IamRoleLoader(IamClientFactory clientFactory, String profile, Region region) {
        super(clientFactory, profile, region);
    }

    @Override
    public IamRole load(IamRoleKey key) {
        var roleName = key.roleName();

        try (var client = createClient()) {
            var awsRole = GetRoleAction.getRole(client, roleName);
            if (awsRole == null) {
                return null;
            }

            var trustPolicy = java.net.URLDecoder.decode(awsRole.assumeRolePolicyDocument(), StandardCharsets.UTF_8);
            return IamRole.builder()
                    .roleName(roleName)
                    .trustPolicy(trustPolicy)
                    .tags(convertTags(awsRole.tags()))
                    .inlinePolicyNames(inlinePolicies(client, roleName))
                    .attachedPolicyArns(AttachPolicyAction.attachedPolicies(client, roleName))
                    .build();
        }
    }

    private static Map<String, String> convertTags(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Map.of();
        }

        return tags.stream().collect(Collectors.toMap(Tag::key, Tag::value));
    }

    private static List<String> inlinePolicies(IamClient client, String roleName) {
        return client.listRolePoliciesPaginator(ListRolePoliciesRequest.builder()
                .roleName(roleName)
                .build()).policyNames().stream().toList();
    }

}
