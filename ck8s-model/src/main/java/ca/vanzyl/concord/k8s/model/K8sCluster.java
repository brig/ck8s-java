package ca.vanzyl.concord.k8s.model;

import ca.vanzyl.concord.k8s.model.aws.Aws;
import ca.vanzyl.concord.k8s.model.network.Network;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableK8sCluster.class)
@JsonDeserialize(as = ImmutableK8sCluster.class)
public interface K8sCluster
        extends K8sRequest
{

    String KIND = "K8sCluster";

    static ImmutableK8sCluster.Builder builder()
    {
        return ImmutableK8sCluster.builder();
    }

    @Override
    @Value.Default
    default String kind()
    {
        return KIND;
    }

    UUID deploymentId();

    @Value.Default
    default K8sClusterStatus status()
    {
        return K8sClusterStatus.UNKNOWN;
    }

    @Value.Default
    default String provider()
    {
        return "aws";
    }

    @Nullable
    Aws aws();

    Set<String> enabledFeatures();

    Set<String> ingressAnnotations();

    Set<String> postManifests();

    Map<String, K8sApplication> applications();

    @Nullable
    Network network();

    @Nullable
    Iam iam();
}
