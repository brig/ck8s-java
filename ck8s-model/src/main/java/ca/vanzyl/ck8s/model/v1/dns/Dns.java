package ca.vanzyl.ck8s.model.v1.dns;

import ca.vanzyl.ck8s.model.v1.dns.cloudflare.Cloudflare;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableDns.class)
@JsonDeserialize(as = ImmutableDns.class)
@JsonPropertyOrder({"provider"})
public interface Dns
{

    static ImmutableDns.Builder builder()
    {
        return ImmutableDns.builder();
    }

    String provider();

    @Nullable
    Cloudflare cloudflare();
}
