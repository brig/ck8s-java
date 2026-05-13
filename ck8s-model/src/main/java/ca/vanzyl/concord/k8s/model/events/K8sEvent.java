package ca.vanzyl.concord.k8s.model.events;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableK8sEvent.class)
@JsonDeserialize(as = ImmutableK8sEvent.class)
public interface K8sEvent
{

    String EVENT_TYPE_ID = "K8S";

    static ImmutableK8sEvent.Builder builder()
    {
        return ImmutableK8sEvent.builder();
    }

    Type type();

    @Value.Default
    default String message()
    {
        return type().format();
    }

    @Value.Default
    default Instant timestamp()
    {
        return Instant.now();
    }

    /**
     * Organization ID in Concord.
     */
    UUID organizationId();

    UUID processId();

    String clusterId();

    @Value.Default
    default Map<String, Object> payload()
    {
        return Collections.emptyMap();
    }

    default Map<String, Object> asMap()
    {
        Map<String, Object> payload = payload();
        return ImmutableMap.<String, Object>builder()
                .put("type", type())
                .put("timestamp", Instant.now().toString())
                .put("processId", processId())
                .put("organizationId", organizationId())
                .put("clusterId", clusterId())
                .put("message", message())
                .put("payload", payload != null ? payload : Collections.emptyMap())
                .build();
    }

    enum Type
    {
        PROCESSING_PROVISIONING_REQUEST("Processing Provisioning Request"),
        PROVISIONING_STARTED("Provisioning Started"),
        APPLICATION_UPDATED("Application Updated: %s"),
        APPLICATION_DELETED("Application Deleted: %s"),
        HELM_INSTALL("Helm install: %s"),
        HELM_UPGRADE("Helm upgrade: %s"),
        PROVISIONING_COMPLETED("Provisioning Completed"),
        PROVISIONING_CANCELLED("Provisioning Canceleld"),
        K8S_CLUSTER_RESOURCES_AVAILABLE("K8s Cluster Resources Available"),
        PROVISIONING_ERROR("Provisioning Error: %s"),
        DESTROY_STARTED("Destroy Started"),
        DESTROY_COMPLETED("Destroy Completed");

        private final String format;

        Type(String format)
        {
            this.format = format;
        }

        public String format()
        {
            return format;
        }
    }
}
