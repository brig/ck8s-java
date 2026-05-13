package ca.vanzyl.ck8s.model.v1.aws;

import ca.vanzyl.ck8s.model.v1.aws.ebs.Ebs;
import ca.vanzyl.ck8s.model.v1.aws.eksctl.Eksctl;
import ca.vanzyl.ck8s.model.v1.aws.iam.Iam;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

import java.util.Map;

@Value.Immutable
@JsonSerialize(as = ImmutableAws.class)
@JsonDeserialize(as = ImmutableAws.class)
@JsonPropertyOrder({
        "provisioner",
        "eksctl",
        "homeRegion",
        "secretsDocument",
        "bucket",
        "sshKeyPair",
        "hostedZoneId",
        "tags"
})
public interface Aws
{

    static ImmutableAws.Builder builder()
    {
        return ImmutableAws.builder();
    }

    String provisioner();

    @Nullable
    Eksctl eksctl();

    String homeRegion();

    String secretsDocument();

    String bucket();

    String sshKeyPair();

    @Nullable
    Iam iam();

    @Nullable
    String hostedZoneId();

    @Nullable
    Map<String, String> tags();

    @Nullable
    Ebs ebs();
}
