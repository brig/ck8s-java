package ca.vanzyl.concord.k8s.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.immutables.value.Value;

import javax.validation.Valid;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(
                value = ImmutableK8sClusterDeployment.class,
                name = K8sClusterDeployment.KIND),
        @JsonSubTypes.Type(
                value = ImmutableK8sCluster.class,
                name = K8sCluster.KIND),
})
public interface K8sRequest
{

    @Value.Default
    default String apiVersion()
    {
        return "ck8s/v1";
    }

    String kind();

    @Valid
    Metadata metadata();
}
