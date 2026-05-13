package ca.vanzyl.ck8s.model.v1.observability.prometheus;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutablePrometheus.class)
@JsonDeserialize(as = ImmutablePrometheus.class)
@JsonPropertyOrder({"remoteWrite"})
public interface Prometheus
{

    static ImmutablePrometheus.Builder builder()
    {
        return ImmutablePrometheus.builder();
    }

    RemoteWrite remoteWrite();
}
