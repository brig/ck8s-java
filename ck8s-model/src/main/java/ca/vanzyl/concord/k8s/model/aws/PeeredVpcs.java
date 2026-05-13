package ca.vanzyl.concord.k8s.model.aws;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutablePeeredVpcs.class)
@JsonDeserialize(as = ImmutablePeeredVpcs.class)
@JsonPropertyOrder({
        "vpc_id",
        "peer_route_ids"
})
public interface PeeredVpcs
{

    static ImmutablePeeredVpcs.Builder builder()
    {
        return ImmutablePeeredVpcs.builder();
    }

    String vpc_id();

    String[] peer_route_ids();
}

