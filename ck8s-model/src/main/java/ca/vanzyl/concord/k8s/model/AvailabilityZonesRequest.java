package ca.vanzyl.concord.k8s.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableAvailabilityZonesRequest.class)
@JsonDeserialize(as = ImmutableAvailabilityZonesRequest.class)
public interface AvailabilityZonesRequest
        extends InfrastructureRequest
{

    static ImmutableAvailabilityZonesRequest.Builder builder()
    {
        return ImmutableAvailabilityZonesRequest.builder();
    }
}
