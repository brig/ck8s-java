package ca.vanzyl.concord.k8s.model.applications;

import ca.vanzyl.concord.k8s.model.NodeGroup;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import javax.annotation.Nullable;

import java.util.Map;

/*
  For the property "name" visible = true is required for that property to be exposed to regular
  BeanDeserializer, which can then map it to existing property. Without this, it's considered
  metadata only, consumed directly by databind itself and then the deserializer will
  consider the property missing causing an error.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(
                value = ImmutableHiveApplicationDeployment.class,
                name = K8sApplicationDeployment.APPLICATION_HIVE_TYPE),
        @JsonSubTypes.Type(
                value = ImmutableApplicationDeployment.class,
                name = K8sApplicationDeployment.APPLICATION_TRINO_TYPE),
        @JsonSubTypes.Type(
                value = ImmutableRangerApplicationDeployment.class,
                name = K8sApplicationDeployment.APPLICATION_RANGER_TYPE),
        @JsonSubTypes.Type(
                value = ImmutableConcordApplicationDeployment.class,
                name = K8sApplicationDeployment.APPLICATION_CONCORD_TYPE),
        @JsonSubTypes.Type(
                value = ImmutableHarborApplicationDeployment.class,
                name = K8sApplicationDeployment.APPLICATION_HARBOR_TYPE)
})
public interface K8sApplicationDeployment // TODO(ib): rename to K8sClusterApplicationDeployment
{

    String APPLICATION_HIVE_TYPE = "hive";
    String APPLICATION_TRINO_TYPE = "trino";
    String APPLICATION_RANGER_TYPE = "ranger";
    String APPLICATION_CONCORD_TYPE = "concord";
    String APPLICATION_HARBOR_TYPE = "harbor";

    String type();

    String version();

    Map<String, String> properties();

    default K8sApplicationDeployment withDefaultValues()
    {
        return this;
    }

    @Nullable
    NodeGroup nodegroup();
}
