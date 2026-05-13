package ca.vanzyl.ck8s.state;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.walmartlabs.concord.svm.ExecutionListener;

import javax.inject.Named;

import static com.google.inject.Scopes.SINGLETON;

@Named
public class StateModule implements com.google.inject.Module {

    @Override
    public void configure (Binder binder) {
        Multibinder<ExecutionListener> executionListeners = Multibinder.newSetBinder(binder, ExecutionListener.class);
        executionListeners.addBinding().to(EntityState.class).in(SINGLETON);
        // TODO: uncomment after priority support in Concord
//        executionListeners.addBinding().to(StateChangesProducer.class).in(SINGLETON);

        var changesProducers = Multibinder.newSetBinder(binder, EntityChangeProducer.class);
        changesProducers.addBinding().to(MapEntityChangesProducer.class);
    }
}
