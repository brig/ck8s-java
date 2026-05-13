package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.preview.Change;
import ca.vanzyl.ck8s.state.EntityChangeProducer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UserPoolUserChangesProducer implements EntityChangeProducer<UserPoolUserKey, UserPoolUser> {

    @Override
    public Class<UserPoolUser> entityType() {
        return UserPoolUser.class;
    }

    @Override
    public List<Change> produce(UserPoolUserKey key, UserPoolUser prev, UserPoolUser current, Instant lastModified) {
        if (prev == null && current == null) {
            return List.of();
        }

        if (current == null) {
            return List.of(Change.delete(CognitoChangeType.userPoolUserId(prev.poolId(), prev.username()))
                    .type(CognitoChangeType.USER_POOL_USER_TYPE)
                    .parentId(CognitoChangeType.userPoolId(prev.poolId()))
                    .metadata(Change.Metadata.builder().name(prev.username()).build())
                    .timestamp(lastModified)
                    .build());
        }

        var action = Change.Action.UPDATE;
        if (prev == null) {
            action = Change.Action.CREATE;
            prev = new UserPoolUser(current.poolId(), null);
        }

        if (prev.equals(current)) {
            return List.of();
        }

        var changes = new ArrayList<Change>();
        changes.add(Change.builder()
                .id(CognitoChangeType.userPoolUserId(current.poolId(), current.username()))
                .parentId(CognitoChangeType.userPoolId(prev.poolId()))
                .action(action)
                .type(CognitoChangeType.USER_POOL_USER_TYPE)
                .metadata(Change.Metadata.builder().name(current.username()).build())
                .timestamp(lastModified)
                .build());
        return changes;
    }
}
