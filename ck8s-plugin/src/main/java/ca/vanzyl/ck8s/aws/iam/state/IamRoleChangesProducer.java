package ca.vanzyl.ck8s.aws.iam.state;

import ca.vanzyl.ck8s.preview.Change;
import ca.vanzyl.ck8s.preview.ChangeUtils;
import ca.vanzyl.ck8s.state.EntityChangeProducer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static ca.vanzyl.ck8s.preview.ChangeUtils.listChanges;
import static ca.vanzyl.ck8s.preview.ChangeUtils.mapChanges;

public class IamRoleChangesProducer implements EntityChangeProducer<IamRoleKey, IamRole> {

    @Override
    public Class<IamRole> entityType() {
        return IamRole.class;
    }

    @Override
    public List<Change> produce(IamRoleKey key, IamRole prev, IamRole current, Instant lastModified) {
        if (prev == null && current == null) {
            return List.of();
        }

        if (current == null) {
            return List.of(Change.delete(IamChangeType.roleId(prev.roleName()))
                    .type(IamChangeType.ROLE_TYPE)
                    .metadata(Change.Metadata.builder().name(prev.roleName()).build())
                    .timestamp(lastModified)
                    .build());
        }

        var action = Change.Action.UPDATE;
        if (prev == null) {
            action = Change.Action.CREATE;
            prev = IamRole.builder().roleName(current.roleName()).build();
        }

        var diff = diff(prev, current);
        if (diff.isEmpty()) {
            return List.of();
        }

        var changes = new ArrayList<Change>();
        changes.add(Change.builder()
                .id(IamChangeType.roleId(current.roleName()))
                .action(action)
                .type(IamChangeType.ROLE_TYPE)
                .metadata(Change.Metadata.builder().name(current.roleName()).build())
                .timestamp(lastModified)
                .build());
        changes.addAll(diff);
        return changes;
    }

    private static List<Change> diff(IamRole stateRole, IamRole role) {
        var roleId = IamChangeType.roleId(role.roleName());

        List<Change> result = new ArrayList<>();
        result.addAll(ChangeUtils.jsonChanges(stateRole.trustPolicy(), role.trustPolicy(), c -> c
                .id(IamChangeType.trustPolicyId(role.roleName()))
                .parentId(roleId)
                .type(IamChangeType.TRUST_POLICY_TYPE)
                .metadata(Change.Metadata.builder().name("").build())));

        result.addAll(mapChanges(roleId, IamChangeType.ROLE_TAG_TYPE,
                stateRole.tags(), role.tags(),
                key -> IamChangeType.roleTagId(role.roleName(), key)));

        result.addAll(listChanges(roleId, IamChangeType.INLINE_POLICY_TYPE,
                stateRole.inlinePolicyNames(), role.inlinePolicyNames(),
                name -> IamChangeType.inlinePolicyId(role.roleName(), name),
                name -> name));

        result.addAll(listChanges(roleId, IamChangeType.MANAGED_POLICY_ATTACH_TYPE,
                stateRole.attachedPolicyArns(), role.attachedPolicyArns(),
                policyArn -> IamChangeType.managedPolicyAttachId(role.roleName(), policyArn),
                IamState::nameFromArn));

        return result;
    }
}
