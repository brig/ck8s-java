package ca.vanzyl.concord.k8s.model.network;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "provider", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(
                value = ImmutableAwsNetwork.class,
                name = AwsNetwork.PROVIDER),
})
public interface Network
{

    String provider();
}
