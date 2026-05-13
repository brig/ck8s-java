package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.preview.Change;
import ca.vanzyl.ck8s.state.EntityChangeProducer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class IdentityProviderChangesProducer implements EntityChangeProducer<IdentityProviderKey, IdentityProvider> {

    @Override
    public Class<IdentityProvider> entityType() {
        return IdentityProvider.class;
    }

    @Override
    public List<Change> produce(IdentityProviderKey key, IdentityProvider prev, IdentityProvider current, Instant lastModified) {
        if (prev == null && current == null) {
            return List.of();
        }

        if (current == null) {
            return List.of(Change.delete(CognitoChangeType.identityProviderId(prev.poolId(), prev.name()))
                    .type(CognitoChangeType.IDENTITY_PROVIDER_TYPE)
                    .parentId(CognitoChangeType.userPoolId(prev.poolId()))
                    .metadata(Change.Metadata.builder().name(prev.name()).build())
                    .timestamp(lastModified)
                    .build());
        }

        var action = Change.Action.UPDATE;
        if (prev == null) {
            action = Change.Action.CREATE;
            prev = new IdentityProvider(current.poolId(), current.name());
        }

// TODO: impl diff
//        var diff = diff(prev, current);
//        if (diff.isEmpty()) {
//            return List.of();
//        }

        var changes = new ArrayList<Change>();
        changes.add(Change.builder()
                .id(CognitoChangeType.identityProviderId(current.poolId(), current.name()))
                .parentId(CognitoChangeType.userPoolId(prev.poolId()))
                .action(action)
                .type(CognitoChangeType.IDENTITY_PROVIDER_TYPE)
                .metadata(Change.Metadata.builder().name(current.name()).build())
                .timestamp(lastModified)
                .build());
//        changes.addAll(diff);
        return changes;
    }
}
