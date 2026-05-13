package ca.vanzyl.ck8s.model.v1.gcp;

import ca.vanzyl.ck8s.model.v1.aws.ImmutableAws;
import ca.vanzyl.ck8s.model.v1.aws.eksctl.Eksctl;
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
        "homeRegion",
        "secretsDocument",
        "bucket",
        "sshKeyPair",
        "tags"
})
public interface Gcp
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
    Map<String, String> tags();
}
