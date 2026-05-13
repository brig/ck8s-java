package ca.vanzyl.ck8s.preview;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Value.Style(jdkOnly = true)
@Value.Immutable
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ImmutableChange.class)
@JsonDeserialize(as = ImmutableChange.class)
public interface Change extends Serializable {

    @Serial
    long serialVersionUID = 1L;

    enum Action {
        CREATE, UPDATE, DELETE
    }

    @Value.Style(jdkOnly = true)
    @Value.Immutable
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonSerialize(as = ImmutableMetadata.class)
    @JsonDeserialize(as = ImmutableMetadata.class)
    interface Metadata extends Serializable {

        @Serial
        long serialVersionUID = 1L;

        @Nullable
        String name();

        static ImmutableMetadata.Builder builder() {
            return ImmutableMetadata.builder();
        }
    }

    Action action();

    String type();

    String id();

    @Value.Default
    default Instant timestamp() {
        return Instant.now();
    }

    @Value.Default
    default Metadata metadata() {
        return Metadata.builder().build();
    }

    @Nullable
    String error();

    @Nullable
    String parentId();

    @Nullable
    String diffMessage();

    static ImmutableChange.Builder builder() {
        return ImmutableChange.builder();
    }

    static ImmutableChange.Builder create(String id) {
        return ImmutableChange.builder()
                .action(Action.CREATE)
                .id(id);
    }

    static ImmutableChange.Builder update(String id) {
        return ImmutableChange.builder()
                .action(Action.UPDATE)
                .id(id);
    }

    static ImmutableChange.Builder delete(String id) {
        return ImmutableChange.builder()
                .action(Action.DELETE)
                .id(id);
    }
}
