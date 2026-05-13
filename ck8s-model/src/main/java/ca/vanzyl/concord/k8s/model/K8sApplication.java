package ca.vanzyl.concord.k8s.model;

import ca.vanzyl.concord.k8s.model.applications.K8sApplicationDeployment;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

import java.util.Collections;
import java.util.Map;

@Value.Immutable
@JsonSerialize(as = ImmutableK8sApplication.class)
@JsonDeserialize(as = ImmutableK8sApplication.class)
public interface K8sApplication
{

    static ImmutableK8sApplication.Builder builder()
    {
        return ImmutableK8sApplication.builder();
    }

    String type();

    @Value.Default
    default K8sApplicationStatus status()
    {
        return K8sApplicationStatus.UNKNOWN;
    }

    @Nullable
    K8sApplicationDeployment deployment();

    // TODO(ib): rename
    @Value.Default
    default Map<String, K8sApplicationStatusEntry> statuses()
    {
        return Collections.emptyMap();
    }
}
