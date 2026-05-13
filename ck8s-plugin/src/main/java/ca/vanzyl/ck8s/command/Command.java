package ca.vanzyl.ck8s.command;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Map;

@Value.Style(jdkOnly = true)
@Value.Immutable
@JsonDeserialize(as = ImmutableCommand.class)
public abstract class Command
{

    @Nullable
    public abstract Map<String, String> env();

    @Nullable
    public abstract String run();

    @Nullable
    public abstract String buildspace();

    @Value.Derived
    public String buildspaceWithDefault()
    {
        return buildspace() != null ? buildspace() : "buildspace";
    }

    @Value.Derived
    public String runWithFormat()
    {
        String run = run();
        run = run.replaceAll("(\r\n|\n)", " ;");
        return run;
    }

    @Nullable
    public abstract String values();
}
