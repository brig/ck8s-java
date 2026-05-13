package ca.vanzyl.concord.k8s.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableTemplateRendererOptions.class)
@JsonDeserialize(as = ImmutableTemplateRendererOptions.class)
public interface TemplateRendererOptions
{

    String DEFAULT_TEMPLATE = "standard";

    static TemplateRendererOptions defaults()
    {
        return builder().build();
    }

    static ImmutableTemplateRendererOptions.Builder builder()
    {
        return ImmutableTemplateRendererOptions.builder();
    }

    @Value.Default
    default String template()
    {
        return DEFAULT_TEMPLATE;
    }

    /**
     * If {@code true}, the renderer produces a version of the flow suitable for local development.
     */
    @Value.Default
    default boolean devMode()
    {
        return false;
    }

    /**
     * Should point to the project's root directory on the local file system. Must be an absolute path, e.g. {@code /home/user/projects/concord-k8s-system}
     */
    @Value.Default
    default String baseDir()
    {
        return System.getProperty("user.dir");
    }
}
