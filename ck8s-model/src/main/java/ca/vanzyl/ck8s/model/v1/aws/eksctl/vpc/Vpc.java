package ca.vanzyl.ck8s.model.v1.aws.eksctl.vpc;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableVpc.class)
@JsonDeserialize(as = ImmutableVpc.class)
@JsonPropertyOrder({"nat"})
public interface Vpc
{

    static ImmutableVpc.Builder builder()
    {
        return ImmutableVpc.builder();
    }

    Nat nat();
}
