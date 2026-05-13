package ca.vanzyl.concord.k8s.model.applications;

import ca.vanzyl.concord.k8s.K8sConverter;
import ca.vanzyl.concord.k8s.model.NodeGroup;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Maps;
import org.immutables.value.Value;

import java.util.HashMap;
import java.util.Map;

@Value.Immutable
@JsonSerialize(as = ImmutableApplicationDeployment.class)
@JsonDeserialize(as = ImmutableApplicationDeployment.class)
public interface ApplicationDeployment
        extends K8sApplicationDeployment
{

    String CATALOG_PROPERTY_CONNECTOR_NAME = "connector.name";

    static ImmutableApplicationDeployment.Builder builder()
    {
        return ImmutableApplicationDeployment.builder();
    }

    @Override
    @Value.Default
    default String type()
    {
        return K8sApplicationDeployment.APPLICATION_TRINO_TYPE;
    }

    default K8sApplicationDeployment withDefaultValues()
    {
        Map<String, String> properties = Maps.newHashMap(properties());

        if (!properties.containsKey("workerCpuRequest")) {
            properties.put("workerCpuRequest", "2");
        }
        if (!properties.containsKey("workerCpuLimit")) {
            properties.put("workerCpuLimit", "3");
        }
        if (!properties.containsKey("workerMemoryRequest")) {
            properties.put("workerMemoryRequest", "2Gi");
        }
        if (!properties.containsKey("workerMemoryLimit")) {
            properties.put("workerMemoryLimit", "3Gi");
        }
        if (!properties.containsKey("coordinatorCpuRequest")) {
            properties.put("coordinatorCpuRequest", "2");
        }
        if (!properties.containsKey("coordinatorCpuLimit")) {
            properties.put("coordinatorCpuLimit", "3");
        }
        if (!properties.containsKey("coordinatorMemoryRequest")) {
            properties.put("coordinatorMemoryRequest", "2Gi");
        }
        if (!properties.containsKey("coordinatorMemoryLimit")) {
            properties.put("coordinatorMemoryLimit", "3Gi");
        }

        return builder()
                .from(this)
                .properties(properties)
                .build();
    }

    // TODO(ib): validate
    String subdomain();

    @Value.Default
    default Map<String, Map<String, String>> catalogs()
    {
        return new HashMap<>();
    }

    @Value.Default
    default Map<String, String> etcFiles()
    {
        return new HashMap<>();
    }

    @Value.Default
    default NodeGroup nodegroup()
    {
        return NodeGroup.builder()
                .minSize(2)
                .maxSize(10)
                .desiredCapacity(3)
                .instanceType("m5.2xlarge")
                .build();
    }

    @Value.Default
    default ExternalSecrets externalSecrets()
    {
        return ImmutableExternalSecrets.builder().build();
    }

    default boolean installLicense()
    {
        return catalogs().values()
                .stream()
                .map(catalogProperties -> catalogProperties.get(CATALOG_PROPERTY_CONNECTOR_NAME))
                .anyMatch("hive-hadoop2"::equals);
    }

    default String catalogsToYaml()
    {
        return K8sConverter.propertiesToYaml(catalogs(), true, 2);
    }

    default String etcFilesToYaml()
    {
        return K8sConverter.textFilesToYaml(etcFiles(), false, 6);
    }
}
