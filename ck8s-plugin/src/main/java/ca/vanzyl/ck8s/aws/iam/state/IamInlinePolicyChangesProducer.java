package ca.vanzyl.ck8s.aws.iam.state;

import ca.vanzyl.ck8s.preview.Change;
import ca.vanzyl.ck8s.preview.ChangeUtils;
import ca.vanzyl.ck8s.state.EntityChangeProducer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class IamInlinePolicyChangesProducer implements EntityChangeProducer<IamInlinePolicyKey, IamInlinePolicy> {

    @Override
    public Class<IamInlinePolicy> entityType() {
        return IamInlinePolicy.class;
    }

    @Override
    public List<Change> produce(IamInlinePolicyKey key, IamInlinePolicy prev, IamInlinePolicy current, Instant lastModified) {
        if (prev == null && current == null) {
            return List.of();
        }

        if (current == null) {
            return List.of(Change.delete(IamChangeType.inlinePolicyId(prev.roleName(), prev.name()))
                    .type(IamChangeType.INLINE_POLICY_TYPE)
                    .parentId(IamChangeType.roleId(prev.roleName()))
                    .metadata(Change.Metadata.builder().name(prev.name()).build())
                    .timestamp(lastModified)
                    .build());
        }

        if (prev == null) {
            prev = new IamInlinePolicy(current.roleName(), current.name(), null);
        }

        return diff(prev, current, lastModified);
    }

    private static List<Change> diff(IamInlinePolicy prev, IamInlinePolicy current, Instant lastModified) {
        if (prev.equals(current)) {
            return List.of();
        }

        List<Change> result = new ArrayList<>();

        result.addAll(ChangeUtils.jsonChanges(prev.document(), current.document(), c -> c
                .id(IamChangeType.inlinePolicyId(current.roleName(), current.name()))
                .type(IamChangeType.INLINE_POLICY_TYPE)
                .parentId(IamChangeType.roleId(current.roleName()))
                .metadata(Change.Metadata.builder().name(current.name()).build())
                .timestamp(lastModified)));

        return result;
    }
}
