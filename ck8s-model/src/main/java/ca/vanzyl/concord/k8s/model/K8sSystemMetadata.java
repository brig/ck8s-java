package ca.vanzyl.concord.k8s.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableK8sSystemMetadata.class)
@JsonDeserialize(as = ImmutableK8sSystemMetadata.class)
public interface K8sSystemMetadata
{

    static ImmutableK8sSystemMetadata.Builder builder()
    {
        return ImmutableK8sSystemMetadata.builder();
    }

    String awsAccountId();
}
