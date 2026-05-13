package ca.vanzyl.ck8s;

import ca.vanzyl.ck8s.preview.Change;
import ca.vanzyl.ck8s.preview.PreviewChangesRecorder;

import java.util.ArrayList;
import java.util.List;

public class MockPreviewRecorder implements PreviewChangesRecorder {

    private final List<Change> changes = new ArrayList<>();

    @Override
    public void record(Change change) {
        System.out.println("record: " + change);
        changes.add(change);
    }

    @Override
    public List<Change> list() {
        return changes;
    }

    @Override
    public void cleanup() {
        changes.clear();
    }
}
