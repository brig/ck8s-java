package ca.vanzyl.concord.k8s.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableNodeGroup.class)
@JsonDeserialize(as = ImmutableNodeGroup.class)
@JsonPropertyOrder({
        "minSize",
        "maxSize",
        "desiredCapacity",
        "instanceType"
})
public interface NodeGroup
{

    static ImmutableNodeGroup.Builder builder()
    {
        return ImmutableNodeGroup.builder();
    }

    @Value.Default
    default int minSize()
    {
        return 2;
    }

    @Value.Default
    default int maxSize()
    {
        return 15;
    }

    @Value.Default
    default int desiredCapacity()
    {
        return minSize();
    }

    @Value.Default
    default String instanceType()
    {
        return "m5.xlarge";
    }

    @Value.Default
    default int volumeSize()
    {
        return 200;
    }

    @Value.Default
    default List<String> availabilityZones()
    {
        return List.of();
    }
}
