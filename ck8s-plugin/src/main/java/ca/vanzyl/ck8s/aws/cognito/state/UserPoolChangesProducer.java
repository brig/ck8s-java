package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.preview.Change;
import ca.vanzyl.ck8s.state.EntityChangeProducer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UserPoolChangesProducer implements EntityChangeProducer<UserPoolKey, UserPool> {

    @Override
    public Class<UserPool> entityType() {
        return UserPool.class;
    }

    @Override
    public List<Change> produce(UserPoolKey key, UserPool prev, UserPool current, Instant lastModified) {
        if (prev == null && current == null) {
            return List.of();
        }

        if (current == null) {
            return List.of(Change.delete(CognitoChangeType.userPoolId(prev.id()))
                    .type(CognitoChangeType.USER_POOL_TYPE)
                    .metadata(Change.Metadata.builder().name(prev.name()).build())
                    .timestamp(lastModified)
                    .build());
        }

        var action = Change.Action.UPDATE;
        if (prev == null) {
            action = Change.Action.CREATE;
            prev = new UserPool(current.id(), current.name());
        }

// TODO: impl diff
//        var diff = diff(prev, current);
//        if (diff.isEmpty()) {
//            return List.of();
//        }

        var changes = new ArrayList<Change>();
        changes.add(Change.builder()
                .id(CognitoChangeType.userPoolId(current.id()))
                .action(action)
                .type(CognitoChangeType.USER_POOL_TYPE)
                .metadata(Change.Metadata.builder().name(current.name()).build())
                .timestamp(lastModified)
                .build());
//        changes.addAll(diff);
        return changes;
    }
}
