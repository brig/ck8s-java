package ca.vanzyl.ck8s.aws.cloudformation.state;

import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationChangeType;
import ca.vanzyl.ck8s.preview.Change;
import ca.vanzyl.ck8s.state.EntityChangeProducer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CloudFormationChangesProducer implements EntityChangeProducer<CloudFormationKey, CloudFormationEntity> {

    @Override
    public Class<CloudFormationEntity> entityType() {
        return CloudFormationEntity.class;
    }

    @Override
    public List<Change> produce(CloudFormationKey key, CloudFormationEntity prev, CloudFormationEntity current, Instant lastModified) {
        if (prev == null && current == null) {
            return List.of();
        }

        if (current == null) {
            return List.of(Change.delete(CloudFormationChangeType.cloudFormationId(key.region(), prev.stackName()))
                    .type(CloudFormationChangeType.CLOUD_FORMATION_TYPE)
                    .metadata(Change.Metadata.builder().name(prev.stackName()).build())
                    .timestamp(lastModified)
                    .build());
        }

        var action = Change.Action.UPDATE;
        if (prev == null) {
            action = Change.Action.CREATE;
        }

        var changes = new ArrayList<Change>();
        changes.add(Change.builder()
                .id(CloudFormationChangeType.cloudFormationId(key.region(), current.stackName()))
                .action(action)
                .type(CloudFormationChangeType.CLOUD_FORMATION_TYPE)
                .metadata(Change.Metadata.builder().name(current.stackName()).build())
                .timestamp(lastModified)
                .build());
        return changes;
    }
}
