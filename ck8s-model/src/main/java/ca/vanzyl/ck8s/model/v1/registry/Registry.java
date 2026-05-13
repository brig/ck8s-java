package ca.vanzyl.ck8s.model.v1.registry;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableRegistry.class)
@JsonDeserialize(as = ImmutableRegistry.class)
@JsonPropertyOrder({"enabled", "host"})
public interface Registry
{

    static ImmutableRegistry.Builder builder()
    {
        return ImmutableRegistry.builder();
    }

    @Value.Default
    default boolean enabled()
    {
        return true;
    }

    String host();
}
