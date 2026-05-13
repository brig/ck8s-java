package ca.vanzyl.ck8s.model.v1.helm;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableHelm.class)
@JsonDeserialize(as = ImmutableHelm.class)
@JsonPropertyOrder({"timeout"})
public interface Helm
{

    static ImmutableHelm.Builder builder()
    {
        return ImmutableHelm.builder();
    }

    String timeout();
}
