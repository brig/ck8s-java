package ca.vanzyl.concord.k8s.model.applications;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableS3Settings.class)
@JsonDeserialize(as = ImmutableS3Settings.class)
public interface S3Settings
{

    static ImmutableS3Settings.Builder builder()
    {
        return ImmutableS3Settings.builder();
    }

    @Value.Default
    default String region()
    {
        return "";
    }

    @Value.Default
    default String accessKeyId()
    {
        return "";
    }

    @Value.Default
    default String secretAccessKey()
    {
        return "";
    }

    @Nullable
    String endpoint();

    @Value.Default
    default boolean pathStyleAccess()
    {
        return false;
    }

    default S3Settings withDefaultValues()
    {
        return this;
    }
}
