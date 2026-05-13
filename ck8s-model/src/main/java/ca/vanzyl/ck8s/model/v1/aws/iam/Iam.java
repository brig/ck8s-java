package ca.vanzyl.ck8s.model.v1.aws.iam;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableIam.class)
@JsonDeserialize(as = ImmutableIam.class)
@JsonPropertyOrder({"ck8sAdminRoleArn"})
public interface Iam
{

    static ImmutableIam.Builder builder()
    {
        return ImmutableIam.builder();
    }

    String ck8sAdminRoleArn();
}
