package ca.vanzyl.ck8s.cloudformation;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.walmartlabs.concord.svm.ExecutionListener;

import javax.inject.Named;

@Named
public class CloudFormationModule implements com.google.inject.Module {

    @Override
    public void configure (Binder binder) {
        Multibinder<ExecutionListener> executionListeners = Multibinder.newSetBinder(binder, ExecutionListener.class);
        executionListeners.addBinding().to(CloudFormation.class);
    }
}
