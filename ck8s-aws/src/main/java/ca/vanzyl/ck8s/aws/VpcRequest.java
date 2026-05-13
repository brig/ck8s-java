package ca.vanzyl.ck8s.aws;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;

@Value.Immutable
@JsonSerialize(as = ImmutableVpcRequest.class)
@JsonDeserialize(as = ImmutableVpcRequest.class)
public abstract class VpcRequest
{

    @Value.Default
    public boolean dryRun()
    {
        return false;
    }

    public abstract String region();

    public abstract String cidrBlock();

    @Value.Default
    public int availabilityZones()
    {
        return 2;
    }

    public abstract List<String> availabilityZoneIds();

    @Value.Default
    public int subnetsPerAvailabilityZone()
    {
        return 2;
    }

    public abstract Map<String, String> tags();
}
