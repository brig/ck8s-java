package ca.vanzyl.concord.k8s.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Map;
import java.util.OptionalInt;

@Value.Immutable
@JsonSerialize(as = ImmutableK8sApplicationStatusEntry.class)
@JsonDeserialize(as = ImmutableK8sApplicationStatusEntry.class)
public interface K8sApplicationStatusEntry
{

    static ImmutableK8sApplicationStatusEntry.Builder builder()
    {
        return ImmutableK8sApplicationStatusEntry.builder();
    }

    Action lastAction();

    Map<String, K8sContainerStatus> containerStatuses();

    OptionalInt replicas();

    OptionalInt readyReplicas();

    OptionalInt availableReplicas();

    enum Action
    {
        UNKNOWN,
        ADDED,
        MODIFIED,
        DELETED,
        ERROR
    }
}
