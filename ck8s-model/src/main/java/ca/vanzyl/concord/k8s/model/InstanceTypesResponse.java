package ca.vanzyl.concord.k8s.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableInstanceTypesResponse.class)
@JsonDeserialize(as = ImmutableInstanceTypesResponse.class)
public interface InstanceTypesResponse
{

    static ImmutableInstanceTypesResponse.Builder builder()
    {
        return ImmutableInstanceTypesResponse.builder();
    }

    List<InstanceType> instanceTypes();
}
