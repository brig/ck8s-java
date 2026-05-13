package ca.vanzyl.ck8s.model.v1.aws;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableServiceAccount.class)
@JsonDeserialize(as = ImmutableServiceAccount.class)
@JsonPropertyOrder({"name", "roleArn"})
public interface ServiceAccount
{

    static ImmutableServiceAccount.Builder builder()
    {
        return ImmutableServiceAccount.builder();
    }

    String name();

    String roleArn();
}
