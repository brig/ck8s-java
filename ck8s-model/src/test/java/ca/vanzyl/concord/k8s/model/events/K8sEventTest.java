package ca.vanzyl.concord.k8s.model.events;

import ca.vanzyl.concord.k8s.ImmutablesJsonMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class K8sEventTest
{
    // This was used to figure out how Jackson's JSR310 time module works.

    @Test
    public void validateSerialization()
            throws Exception
    {

        Instant timestamp = Instant.now();

        Map<String, Object> data = ImmutableMap.<String, Object>builder()
                .put("type", K8sEvent.Type.PROVISIONING_STARTED)
                .put("timestamp", timestamp.toString())
                .put("organizationId", "f2563648-d8cd-11ea-a947-8f06790e96aa")
                .put("processId", "214e4405-fded-4e1b-831d-e34307e96f7f")
                .put("clusterId", "test-cluster-id")
                .put("message", "Provisioning Started")
                .build();

        ImmutablesJsonMapper mapper = new ImmutablesJsonMapper();
        K8sEvent event = mapper.convert(data, K8sEvent.class);

        assertThat(event.type()).isEqualTo(K8sEvent.Type.PROVISIONING_STARTED);
        assertThat(event.timestamp()).isEqualTo(timestamp);
        assertThat(event.organizationId()).isEqualTo(UUID.fromString("f2563648-d8cd-11ea-a947-8f06790e96aa"));
        assertThat(event.processId()).isEqualTo(UUID.fromString("214e4405-fded-4e1b-831d-e34307e96f7f"));
        assertThat(event.clusterId()).isEqualTo("test-cluster-id");
        assertThat(event.message()).isEqualTo("Provisioning Started");
    }
}
