package ca.vanzyl.ck8s.aws;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Map;

import org.immutables.value.Value;
import software.amazon.awssdk.services.ec2.model.VolumeType;

@Value.Immutable
@JsonSerialize(as = ImmutableInstanceBuilderRequest.class)
@JsonDeserialize(as = ImmutableInstanceBuilderRequest.class)
public abstract class InstanceBuilderRequest
{

    public abstract String region();

    public abstract String account();

    public abstract String instanceProfile();

    @Value.Default
    public String architecture()
    {
        return "amd64";
    }

    public abstract String sshKeyPair();

    @Value.Default
    public int volumeSize()
    {
        return 50;
    }

    @Value.Default
    public VolumeType volumeType()
    {
        return VolumeType.GP2;
    }

    public abstract Map<String, String> tags();
}
