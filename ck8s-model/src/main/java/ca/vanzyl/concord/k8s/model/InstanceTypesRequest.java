package ca.vanzyl.concord.k8s.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableInstanceTypesRequest.class)
@JsonDeserialize(as = ImmutableInstanceTypesRequest.class)
public interface InstanceTypesRequest
        extends InfrastructureRequest
{

    static ImmutableInstanceTypesRequest.Builder builder()
    {
        return ImmutableInstanceTypesRequest.builder();
    }

    @Value.Default
    default int minCpus()
    {
        return 4;
    }

    @Value.Default
    default long minSizeInMiB()
    {
        return 16384;   // 16gb
    }
}
