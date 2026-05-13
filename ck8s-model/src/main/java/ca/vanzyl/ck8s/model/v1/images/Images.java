package ca.vanzyl.ck8s.model.v1.images;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableImages.class)
@JsonDeserialize(as = ImmutableImages.class)
@JsonPropertyOrder({"pullPolicy"})
public interface Images
{

    static ImmutableImages.Builder builder()
    {
        return ImmutableImages.builder();
    }

    String pullPolicy();
}
