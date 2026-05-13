package ca.vanzyl.ck8s.aws;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.walmartlabs.concord.svm.ExecutionListener;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
public class AwsTasksModule implements com.google.inject.Module {

    @Override
    public void configure (Binder binder){
        Multibinder<ExecutionListener> executionListenerBinder = Multibinder.newSetBinder(binder, ExecutionListener.class);
        executionListenerBinder.addBinding().to(CredentialsProvider.class).in(Singleton.class);
    }
}
