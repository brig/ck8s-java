package ca.vanzyl.ck8s.preview;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Map;

@Value.Style(jdkOnly = true)
@Value.Immutable
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableProcessInfo.class)
@JsonDeserialize(as = ImmutableProcessInfo.class)
public interface ProcessInfo {

    String cluster();

    String flow();

    @Value.Default
    default Map<String, Object> args() {
        return Map.of();
    }

    static ImmutableProcessInfo.Builder builder() {
        return ImmutableProcessInfo.builder();
    }
}
