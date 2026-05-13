package ca.vanzyl.ck8s.preview;

import java.util.List;
import java.util.function.Consumer;

public interface PreviewChangesRecorder {

    void record(Change change);

    default void record(Consumer<ImmutableChange.Builder> change) {
        var builder = Change.builder();
        change.accept(builder);
        record(builder.build());
    }

    default void record(List<Change> changes) {
        changes.forEach(this::record);
    }

    List<Change> list();

    void cleanup();
}
