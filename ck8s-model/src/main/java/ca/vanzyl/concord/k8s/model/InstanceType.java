package ca.vanzyl.concord.k8s.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableInstanceType.class)
@JsonDeserialize(as = ImmutableInstanceType.class)
public interface InstanceType
{

    String type();

    int cpus();

    long sizeInMiB();

    boolean currentGeneration();
}
