package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.preview.Change;
import ca.vanzyl.ck8s.state.EntityChangeProducer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UserPoolClientChangesProducer implements EntityChangeProducer<UserPoolClientKey, UserPoolClient> {

    @Override
    public Class<UserPoolClient> entityType() {
        return UserPoolClient.class;
    }

    @Override
    public List<Change> produce(UserPoolClientKey key, UserPoolClient prev, UserPoolClient current, Instant lastModified) {
        if (prev == null && current == null) {
            return List.of();
        }

        if (current == null) {
            return List.of(Change.delete(CognitoChangeType.userPoolClientId(prev.poolId(), prev.clientName()))
                    .type(CognitoChangeType.USER_POOL_CLIENT_TYPE)
                    .parentId(CognitoChangeType.userPoolId(prev.poolId()))
                    .metadata(Change.Metadata.builder().name(prev.clientName()).build())
                    .timestamp(lastModified)
                    .build());
        }

        var action = Change.Action.UPDATE;
        if (prev == null) {
            action = Change.Action.CREATE;
            prev = new UserPoolClient(current.poolId(), current.clientId(), current.clientName());
        }

// TODO: impl diff
//        var diff = diff(prev, current);
//        if (diff.isEmpty()) {
//            return List.of();
//        }

        var changes = new ArrayList<Change>();
        changes.add(Change.builder()
                .id(CognitoChangeType.userPoolClientId(current.poolId(), current.clientName()))
                .parentId(CognitoChangeType.userPoolId(prev.poolId()))
                .action(action)
                .type(CognitoChangeType.USER_POOL_CLIENT_TYPE)
                .metadata(Change.Metadata.builder().name(current.clientName()).build())
                .timestamp(lastModified)
                .build());
//        changes.addAll(diff);
        return changes;
    }
}
