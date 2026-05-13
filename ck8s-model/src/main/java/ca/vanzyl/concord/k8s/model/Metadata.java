package ca.vanzyl.concord.k8s.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.OffsetDateTime;

@Value.Immutable
@JsonSerialize(as = ImmutableMetadata.class)
@JsonDeserialize(as = ImmutableMetadata.class)
public interface Metadata
{

    static ImmutableMetadata.Builder builder()
    {
        return ImmutableMetadata.builder();
    }

    @Value.Default
    default boolean debug()
    {
        return false;
    }

    @Value.Default
    default OffsetDateTime createdAt()
    {
        return OffsetDateTime.now();
    }

    @Value.Default
    default OffsetDateTime updatedAt()
    {
        return OffsetDateTime.now();
    }

    String profile();

    @ValidSubdomain
    String name();

    @Value.Default
    default String organization()
    {
        return "myco";
    }

    @Value.Default
    default String project()
    {
        return "concord-k8s-builder";
    }
}
