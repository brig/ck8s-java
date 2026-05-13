package ca.vanzyl.ck8s.aws;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableSubnet.class)
@JsonDeserialize(as = ImmutableSubnet.class)
public abstract class Subnet
{
    public abstract String id();

    public abstract String cidrBlock();

    public abstract String availabilityZoneId();

    public abstract boolean publiclyAccessible();
}
