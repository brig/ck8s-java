package ca.vanzyl.ck8s.k8s;

import ca.vanzyl.ck8s.k8s.actions.*;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;

import javax.inject.Named;

@Named
public class K8sTaskModule implements com.google.inject.Module {

    @Override
    public void configure (Binder binder) {
        var actions = Multibinder.newSetBinder(binder, K8sTaskAction.class);
        actions.addBinding().to(CreateNamespaceAction.class);
        actions.addBinding().to(CreateNamespacePreviewAction.class);
        actions.addBinding().to(ExistsNamespaceAction.class);
        actions.addBinding().to(DeleteNamespaceAction.class);
        actions.addBinding().to(DeleteNamespacePreviewAction.class);
        actions.addBinding().to(GetSecretDataAction.class);
        actions.addBinding().to(UpsertSecretAction.class);
        actions.addBinding().to(GetPodsAction.class);
        actions.addBinding().to(ListEventsAction.class);
        actions.addBinding().to(PodLogsAction.class);
    }
}
