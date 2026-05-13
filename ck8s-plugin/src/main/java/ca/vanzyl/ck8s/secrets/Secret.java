package ca.vanzyl.ck8s.secrets;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Style(jdkOnly = true)
@Value.Immutable
@JsonDeserialize(as = ImmutableSecret.class)
@JsonSerialize(as = ImmutableSecret.class)
public abstract class Secret
{

    public abstract String name();

    public abstract String value();

    @Nullable
    public abstract String description();
}
