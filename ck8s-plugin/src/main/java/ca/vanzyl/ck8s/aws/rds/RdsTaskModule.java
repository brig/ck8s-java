package ca.vanzyl.ck8s.aws.rds;

import ca.vanzyl.ck8s.aws.rds.actions.FetchEndpointAction;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;

import javax.inject.Named;

@Named
public class RdsTaskModule implements com.google.inject.Module {

    @Override
    public void configure (Binder binder) {
        var actions = Multibinder.newSetBinder(binder, RdsTaskAction.class);
        actions.addBinding().to(FetchEndpointAction.class);
    }
}
