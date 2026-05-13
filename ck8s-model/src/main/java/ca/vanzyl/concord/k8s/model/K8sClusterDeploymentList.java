package ca.vanzyl.concord.k8s.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableK8sClusterDeploymentList.class)
@JsonDeserialize(as = ImmutableK8sClusterDeploymentList.class)
public interface K8sClusterDeploymentList
{

    static ImmutableK8sClusterDeploymentList.Builder builder()
    {
        return ImmutableK8sClusterDeploymentList.builder();
    }

    List<UUID> deploymentIds();
}
