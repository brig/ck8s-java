package ca.vanzyl.ck8s.aws.efs;

import ca.vanzyl.ck8s.aws.efs.actions.CreateAccessPointAction;
import ca.vanzyl.ck8s.aws.efs.actions.DeleteAccessPointAction;
import ca.vanzyl.ck8s.aws.efs.actions.FindAccessPointAction;
import ca.vanzyl.ck8s.aws.efs.actions.FindFileSystemAction;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;

import javax.inject.Named;

@Named
public class EfsTaskModule implements com.google.inject.Module {

    @Override
    public void configure (Binder binder) {
        var actions = Multibinder.newSetBinder(binder, EfsTaskAction.class);
        actions.addBinding().to(FindAccessPointAction.class);
        actions.addBinding().to(FindFileSystemAction.class);
        actions.addBinding().to(CreateAccessPointAction.class);
        actions.addBinding().to(DeleteAccessPointAction.class);
    }
}
