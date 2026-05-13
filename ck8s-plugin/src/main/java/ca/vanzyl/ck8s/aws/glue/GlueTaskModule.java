package ca.vanzyl.ck8s.aws.glue;

import ca.vanzyl.ck8s.aws.glue.actions.ExistsDatabaseAction;
import ca.vanzyl.ck8s.aws.glue.actions.ExistsDatabaseActionInAwsPermissionsPhase;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;

import javax.inject.Named;

@Named
public class GlueTaskModule implements com.google.inject.Module {

    @Override
    public void configure (Binder binder) {
        var actions = Multibinder.newSetBinder(binder, GlueTaskAction.class);
        actions.addBinding().to(ExistsDatabaseAction.class);
        actions.addBinding().to(ExistsDatabaseActionInAwsPermissionsPhase.class);
    }
}
