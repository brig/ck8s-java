package ca.vanzyl.ck8s.preview;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.walmartlabs.concord.svm.ExecutionListener;

import javax.inject.Named;

import static com.google.inject.Scopes.SINGLETON;

@Named
public class PreviewModule implements com.google.inject.Module {

    @Override
    public void configure (Binder binder) {
        binder.bind(PreviewChangesRecorder.class).to(DefaultPreviewChangesRecorder.class).in(SINGLETON);;

        Multibinder<ExecutionListener> executionListeners = Multibinder.newSetBinder(binder, ExecutionListener.class);
        executionListeners.addBinding().to(PreviewReporter.class);
    }
}
