package ca.vanzyl.concord.k8s.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableK8sOperationResponse.class)
@JsonDeserialize(as = ImmutableK8sOperationResponse.class)
public interface K8sOperationResponse
{

    static ImmutableK8sOperationResponse.Builder builder()
    {
        return ImmutableK8sOperationResponse.builder();
    }

    /**
     * ID of a Concord process that was created by the operation.
     */
    UUID processId();

    @Value.Default
    default int status()
    {
        return 200;
    }
}
