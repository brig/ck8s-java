package ca.vanzyl.ck8s.model.v1.aws.efs;

import ca.vanzyl.ck8s.model.v1.aws.ServiceAccount;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableEfs.class)
@JsonDeserialize(as = ImmutableEfs.class)
@JsonPropertyOrder({"enabled", "serviceAccount", "volumeId"})
public interface Efs
{

    static ImmutableEfs.Builder builder()
    {
        return ImmutableEfs.builder();
    }

    boolean enabled();

    ServiceAccount serviceAccount();

    @Nullable
    String volumeId();
}
