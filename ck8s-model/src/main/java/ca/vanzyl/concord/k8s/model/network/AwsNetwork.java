package ca.vanzyl.concord.k8s.model.network;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableAwsNetwork.class)
@JsonDeserialize(as = ImmutableAwsNetwork.class)
public interface AwsNetwork
        extends Network
{

    String PROVIDER = "aws";

    @Override
    @Value.Default
    default String provider()
    {
        return PROVIDER;
    }

    String vpcId();

    String cidrBlock();
}
