package ca.vanzyl.concord.k8s.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableAvailabilityZonesResponse.class)
@JsonDeserialize(as = ImmutableAvailabilityZonesResponse.class)
public interface AvailabilityZonesResponse
{

    static ImmutableAvailabilityZonesResponse.Builder builder()
    {
        return ImmutableAvailabilityZonesResponse.builder();
    }

    List<String> availabilityZones();
}
