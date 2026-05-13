package ca.vanzyl.ck8s.aws;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;

@Value.Immutable
@JsonSerialize(as = ImmutableVpcResult.class)
@JsonDeserialize(as = ImmutableVpcResult.class)
public abstract class VpcResult
{
    public abstract String region();

    public abstract String cidrBlock();

    public abstract Map<String, String> tags();

    public abstract String vpcId();

    public abstract List<Subnet> subnets();

    public abstract List<Subnet> publiclyAccessibleSubnets();
}
