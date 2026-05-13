package ca.vanzyl.ck8s.model.v1.aws.eksctl;

import ca.vanzyl.ck8s.model.v1.aws.eksctl.iam.Iam;
import ca.vanzyl.ck8s.model.v1.aws.eksctl.vpc.Vpc;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableEksctl.class)
@JsonDeserialize(as = ImmutableEksctl.class)
@JsonPropertyOrder({"version", "vpc", "iam"})
public interface Eksctl
{

    static ImmutableEksctl.Builder builder()
    {
        return ImmutableEksctl.builder();
    }

    String version();

    Vpc vpc();

    Iam iam();
}
