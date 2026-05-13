package ca.vanzyl.ck8s.secrets;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Style(jdkOnly = true)
@Value.Immutable
@JsonDeserialize(as = ImmutableSecrets.class)
@JsonSerialize(as = ImmutableSecrets.class)
public abstract class Secrets
{

    @JsonProperty("secrets")
    public abstract List<Secret> list();
}
