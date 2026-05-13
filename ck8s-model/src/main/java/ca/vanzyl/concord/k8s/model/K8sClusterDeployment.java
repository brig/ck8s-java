package ca.vanzyl.concord.k8s.model;

import ca.vanzyl.concord.k8s.model.applications.K8sApplicationDeployment;
import ca.vanzyl.concord.k8s.model.aws.Aws;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableK8sClusterDeployment.class)
@JsonDeserialize(as = ImmutableK8sClusterDeployment.class)
@JsonPropertyOrder({
        "kind",
        "metadata",
        "deploymentId",
        "domain",
        "environment",
        "http01solver",
        "flow",
        "provider",
        "account",
        "user",
        "aws",
        "nodegroup",
        "applications"
})
public interface K8sClusterDeployment
        extends K8sRequest
{

    String KIND = "K8sClusterDeployment";

    static ImmutableK8sClusterDeployment.Builder builder()
    {
        return ImmutableK8sClusterDeployment.builder();
    }

    @Override
    @Value.Default
    default String kind()
    {
        return KIND;
    }

    @Nullable
    UUID deploymentId();

    // TODO(ib): status?

    // TODO(ib): validate
    @Nullable
    String domain();

    @Value.Default
    default String environment()
    {
        return "production";
    }

    @Value.Default
    default boolean http01solver()
    {
        return false;
    }

    @Nullable
    String flow();

    @Value.Default
    default Map<@ValidSubdomain String, K8sApplicationDeployment> applications()
    {
        return Collections.emptyMap();
    }

    @SuppressWarnings("unused")
    default boolean hasApplicationsToProvision()
    {
        Map<String, K8sApplicationDeployment> applications = applications();
        return applications != null && applications.size() > 0;
    }

    @Value.Default
    default String provider()
    {
        if (aws() != null) {
            return "aws";
        }
        return "aws";
    }

    @Value.Default
    default String account()
    {
        return "experimentation";
    }

    @Value.Default
    default String user()
    {
        return "automation";
    }

    @Nullable
    Aws aws();

    @Value.Default
    default NodeGroup nodegroup()
    {
        return NodeGroup.builder().build();
    }
}
