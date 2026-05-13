package ca.vanzyl.concord.k8s.model;

import ca.vanzyl.concord.k8s.model.applications.K8sApplicationDeployment;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableK8sClusterApplicationDeploymentList.class)
@JsonDeserialize(as = ImmutableK8sClusterApplicationDeploymentList.class)
public interface K8sClusterApplicationDeploymentList
{

    static ImmutableK8sClusterApplicationDeploymentList.Builder builder()
    {
        return ImmutableK8sClusterApplicationDeploymentList.builder();
    }

    @Value.Default
    default List<K8sApplicationDeployment> applications()
    {
        return new ArrayList<>();
    }
}
