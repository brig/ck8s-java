package ca.vanzyl.ck8s.model.v1.dns.cloudflare;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudflare.class)
@JsonDeserialize(as = ImmutableCloudflare.class)
@JsonPropertyOrder({"proxied"})
public interface Cloudflare
{

    static ImmutableCloudflare.Builder builder()
    {
        return ImmutableCloudflare.builder();
    }

    boolean proxied();
}
