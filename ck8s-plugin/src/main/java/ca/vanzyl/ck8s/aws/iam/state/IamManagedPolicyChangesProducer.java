package ca.vanzyl.ck8s.aws.iam.state;

import ca.vanzyl.ck8s.preview.Change;
import ca.vanzyl.ck8s.preview.ChangeUtils;
import ca.vanzyl.ck8s.state.EntityChangeProducer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class IamManagedPolicyChangesProducer implements EntityChangeProducer<IamManagedPolicyKey, IamManagedPolicy> {

    @Override
    public Class<IamManagedPolicy> entityType() {
        return IamManagedPolicy.class;
    }

    @Override
    public List<Change> produce(IamManagedPolicyKey key, IamManagedPolicy prev, IamManagedPolicy current, Instant lastModified) {
        if (prev == null && current == null) {
            return List.of();
        }

        if (current == null) {
            return List.of(Change.delete(IamChangeType.managedPolicyId(prev.arn()))
                    .type(IamChangeType.MANAGED_POLICY_TYPE)
                    .metadata(Change.Metadata.builder().name(prev.name()).build())
                    .timestamp(lastModified)
                    .build());
        }

        if (prev == null) {
            prev = new IamManagedPolicy(current.arn(), current.name(), null);
        }

        return diff(prev, current, lastModified);
    }

    private static List<Change> diff(IamManagedPolicy prev, IamManagedPolicy current, Instant lastModified) {
        if (prev.equals(current)) {
            return List.of();
        }

        List<Change> result = new ArrayList<>();

        result.addAll(ChangeUtils.jsonChanges(prev.document(), current.document(), c -> c
                .id(IamChangeType.managedPolicyId(current.arn()))
                .type(IamChangeType.MANAGED_POLICY_TYPE)
                .metadata(Change.Metadata.builder().name(current.name()).build())
                .timestamp(lastModified)));

        return result;
    }
}
