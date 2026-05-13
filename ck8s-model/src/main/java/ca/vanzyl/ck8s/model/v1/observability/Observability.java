package ca.vanzyl.ck8s.model.v1.observability;

import ca.vanzyl.ck8s.model.v1.observability.prometheus.Prometheus;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableObservability.class)
@JsonDeserialize(as = ImmutableObservability.class)
@JsonPropertyOrder({"namespace", "prometheus"})
public interface Observability
{

    static ImmutableObservability.Builder builder()
    {
        return ImmutableObservability.builder();
    }

    String namespace();

    Prometheus prometheus();
}
