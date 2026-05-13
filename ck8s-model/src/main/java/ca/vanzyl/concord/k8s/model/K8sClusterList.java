package ca.vanzyl.concord.k8s.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableK8sClusterList.class)
@JsonDeserialize(as = ImmutableK8sClusterList.class)
public interface K8sClusterList
{

    static ImmutableK8sClusterList.Builder builder()
    {
        return ImmutableK8sClusterList.builder();
    }

    List<String> clusterIds();
}
