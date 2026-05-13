package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.preview.Change;
import ca.vanzyl.ck8s.state.EntityChangeProducer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ResourceServerChangesProducer implements EntityChangeProducer<ResourceServerKey, ResourceServer> {

    @Override
    public Class<ResourceServer> entityType() {
        return ResourceServer.class;
    }

    @Override
    public List<Change> produce(ResourceServerKey key, ResourceServer prev, ResourceServer current, Instant lastModified) {
        if (prev == null && current == null) {
            return List.of();
        }

        if (current == null) {
            return List.of(Change.delete(CognitoChangeType.resourceServerId(prev.poolId(), prev.identifier()))
                    .type(CognitoChangeType.RESOURCE_SERVER_TYPE)
                    .parentId(CognitoChangeType.userPoolId(prev.poolId()))
                    .metadata(Change.Metadata.builder().name(prev.name()).build())
                    .timestamp(lastModified)
                    .build());
        }

        var action = Change.Action.UPDATE;
        if (prev == null) {
            action = Change.Action.CREATE;
            prev = new ResourceServer(current.poolId(), current.identifier(), current.name());
        }

// TODO: impl diff
//        var diff = diff(prev, current);
//        if (diff.isEmpty()) {
//            return List.of();
//        }

        var changes = new ArrayList<Change>();
        changes.add(Change.builder()
                .id(CognitoChangeType.resourceServerId(current.poolId(), current.identifier()))
                .parentId(CognitoChangeType.userPoolId(prev.poolId()))
                .action(action)
                .type(CognitoChangeType.RESOURCE_SERVER_TYPE)
                .metadata(Change.Metadata.builder().name(current.name()).build())
                .timestamp(lastModified)
                .build());
//        changes.addAll(diff);
        return changes;
    }
}
