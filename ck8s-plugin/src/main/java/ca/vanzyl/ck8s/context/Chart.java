package ca.vanzyl.ck8s.context;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Style(jdkOnly = true)
@Value.Immutable
@JsonDeserialize(as = ImmutableChart.class)
public abstract class Chart
{

    public abstract String name();

    public abstract String version();

    public abstract String namespace();
}
