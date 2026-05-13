package ca.vanzyl.concord.k8s.model.applications;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableConcordApplicationDeployment.class)
@JsonDeserialize(as = ImmutableConcordApplicationDeployment.class)
public interface ConcordApplicationDeployment
        extends K8sApplicationDeployment
{

    @Override
    @Value.Default
    default String type()
    {
        return K8sApplicationDeployment.APPLICATION_CONCORD_TYPE;
    }

    //TODO(acz) : Version is hardcoded in flow - once flow updated we can remove default ""
    @Override
    @Value.Default
    default String version()
    {
        return "";
    }
}
