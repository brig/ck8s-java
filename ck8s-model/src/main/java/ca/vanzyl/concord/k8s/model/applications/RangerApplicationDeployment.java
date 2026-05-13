package ca.vanzyl.concord.k8s.model.applications;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableRangerApplicationDeployment.class)
@JsonDeserialize(as = ImmutableRangerApplicationDeployment.class)
public interface RangerApplicationDeployment
        extends K8sApplicationDeployment
{

    static ImmutableRangerApplicationDeployment.Builder builder()
    {
        return ImmutableRangerApplicationDeployment.builder();
    }

    @Override
    @Value.Default
    default String type()
    {
        return K8sApplicationDeployment.APPLICATION_RANGER_TYPE;
    }
}
