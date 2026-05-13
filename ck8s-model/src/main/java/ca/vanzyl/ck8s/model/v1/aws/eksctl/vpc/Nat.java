package ca.vanzyl.ck8s.model.v1.aws.eksctl.vpc;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableNat.class)
@JsonDeserialize(as = ImmutableNat.class)
@JsonPropertyOrder({"gateway"})
public interface Nat
{

    static ImmutableNat.Builder builder()
    {
        return ImmutableNat.builder();
    }

    String gateway();
}
