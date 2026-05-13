package ca.vanzyl.concord.k8s.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableIam.class)
@JsonDeserialize(as = ImmutableIam.class)

public interface Iam
{

    static ImmutableIam.Builder builder()
    {
        return ImmutableIam.builder();
    }

    String instanceRoleArn();

    String instanceProfileArn();
}
