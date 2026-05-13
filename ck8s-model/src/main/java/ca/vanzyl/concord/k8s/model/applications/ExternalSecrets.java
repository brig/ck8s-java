package ca.vanzyl.concord.k8s.model.applications;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableExternalSecrets.class)
@JsonDeserialize(as = ImmutableExternalSecrets.class)
public interface ExternalSecrets
{

    @Value.Default
    default boolean enabled()
    {
        return false;
    }

    @Value.Default
    default String secretsPrefix()
    {
        return "";
    }
}
