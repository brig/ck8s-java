package ca.vanzyl.ck8s.aws.asm;

import ca.vanzyl.ck8s.aws.asm.actions.CreateSecretAction;
import ca.vanzyl.ck8s.aws.asm.actions.DeleteSecretAction;
import ca.vanzyl.ck8s.aws.asm.actions.GetSecretAction;
import ca.vanzyl.ck8s.aws.asm.actions.UpdateSecretAction;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;

import javax.inject.Named;

@Named
public class AsmTaskModule implements com.google.inject.Module {

    @Override
    public void configure(Binder binder) {
        var actions = Multibinder.newSetBinder(binder, AsmTaskAction.class);
        actions.addBinding().to(CreateSecretAction.class);
        actions.addBinding().to(GetSecretAction.class);
        actions.addBinding().to(UpdateSecretAction.class);
        actions.addBinding().to(DeleteSecretAction.class);
    }
}