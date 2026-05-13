package ca.vanzyl.concord.k8s.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableK8sClusterApplicationList.class)
@JsonDeserialize(as = ImmutableK8sClusterApplicationList.class)
public interface K8sClusterApplicationList
{

    static ImmutableK8sClusterApplicationList.Builder builder()
    {
        return ImmutableK8sClusterApplicationList.builder();
    }

    @Value.Default
    default List<K8sApplication> applications()
    {
        return new ArrayList<>();
    }
}
