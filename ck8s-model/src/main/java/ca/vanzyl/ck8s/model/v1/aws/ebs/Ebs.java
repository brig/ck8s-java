package ca.vanzyl.ck8s.model.v1.aws.ebs;

import ca.vanzyl.ck8s.model.v1.aws.ServiceAccount;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableEbs.class)
@JsonDeserialize(as = ImmutableEbs.class)
@JsonPropertyOrder({"enabled", "serviceAccount"})
public interface Ebs
{

    static ImmutableEbs.Builder builder()
    {
        return ImmutableEbs.builder();
    }

    boolean enabled();

    ServiceAccount serviceAccount();
}
