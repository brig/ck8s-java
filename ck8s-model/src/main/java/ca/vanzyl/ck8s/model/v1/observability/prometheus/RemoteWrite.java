package ca.vanzyl.ck8s.model.v1.observability.prometheus;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableRemoteWrite.class)
@JsonDeserialize(as = ImmutableRemoteWrite.class)
@JsonPropertyOrder({"url", "bearerToken"})
public interface RemoteWrite
{

    static ImmutableRemoteWrite.Builder builder()
    {
        return ImmutableRemoteWrite.builder();
    }

    String url();

    String bearerToken();
}
