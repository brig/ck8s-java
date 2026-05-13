package ca.vanzyl.concord.k8s.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableK8sContainerStatus.class)
@JsonDeserialize(as = ImmutableK8sContainerStatus.class)
public interface K8sContainerStatus
{

    State state();

    String message();

    enum State
    {
        UNKNOWN,
        RUNNING,
        WAITING,
        TERMINATED
    }
}
