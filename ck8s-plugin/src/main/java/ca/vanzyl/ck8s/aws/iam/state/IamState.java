package ca.vanzyl.ck8s.aws.iam.state;

import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskParams;
import ca.vanzyl.ck8s.state.EntityState;
import software.amazon.awssdk.services.iam.model.ListEntitiesForPolicyRequest;
import software.amazon.awssdk.services.iam.model.NoSuchEntityException;
import software.amazon.awssdk.services.iam.model.PolicyRole;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class IamState {

    private final IamClientFactory clientFactory;
    private final EntityState state;

    @Inject
    public IamState(EntityState state, IamClientFactory clientFactory) {
        this.state = state;
        this.clientFactory = clientFactory;
    }

    public IamRole role(IamTaskParams.BaseParams baseParams, String roleName) {
        return state.getOrLoad(new IamRoleKey(roleName),
                new IamRoleLoader(clientFactory, baseParams.profile(), baseParams.region()));
    }

    public IamManagedPolicy managedPolicy(IamTaskParams.BaseParams baseParams, String policyArn) {
        return state.getOrLoad(new IamManagedPolicyKey(policyArn),
                new IamManagedPolicyLoader(clientFactory, baseParams.profile(), baseParams.region()));
    }

    public IamInlinePolicy inlinePolicy(IamTaskParams.BaseParams baseParams, String roleName, String policyName) {
        return state.getOrLoad(new IamInlinePolicyKey(roleName, policyName),
                new IamInlinePolicyLoader(clientFactory, baseParams.profile(), baseParams.region()));
    }

    public void put(IamRole role) {
        state.put(new IamRoleKey(role.roleName()), role);
    }

    public void put(IamManagedPolicy managedPolicy) {
        state.put(new IamManagedPolicyKey(managedPolicy.arn()), managedPolicy);
    }

    public void put(String roleName, IamInlinePolicy inlinePolicy) {
        state.put(new IamInlinePolicyKey(roleName, inlinePolicy.name()), inlinePolicy);
    }

    public void deleteRole(String roleName) {
        state.delete(new IamRoleKey(roleName));
    }

    public void deleteManagedPolicy(String policyArn) {
        state.delete(new IamManagedPolicyKey(policyArn));
    }

    public List<IamRole> listRolesForPolicy(IamTaskParams.BaseParams baseParams, String policyArn) {
        Set<String> awsPolicyRoles = Set.of();
        try (var client = clientFactory.create(baseParams.profile(), baseParams.region())) {
            awsPolicyRoles = client.listEntitiesForPolicyPaginator(
                            ListEntitiesForPolicyRequest.builder()
                                    .policyArn(policyArn)
                                    .build())
                    .policyRoles().stream()
                    .map(PolicyRole::roleName)
                    .collect(Collectors.toSet());
        } catch (NoSuchEntityException e) {
            // ignore
        }

        var result = new HashSet<>(awsPolicyRoles);
        for (var r : state.<IamRoleKey, IamRole>list(IamRole.class)) {
            if (r.getValue() == null) {
                result.remove(r.getKey().roleName());
            } else {
                result.add(r.getKey().roleName());
            }
        }

        return result.stream()
                .map(roleName -> role(baseParams, roleName))
                .filter(Objects::nonNull)
                .toList();
    }

    public static String nameFromArn(String policyArn) {
        int lastSlash = policyArn.lastIndexOf('/');
        if (lastSlash == -1 || lastSlash == policyArn.length() - 1) {
            return policyArn;
        }
        return policyArn.substring(lastSlash + 1);
    }

}
