package ca.vanzyl.ck8s.aws.cloudformation;

import ca.vanzyl.ck8s.aws.cloudformation.actions.*;
import ca.vanzyl.ck8s.aws.cloudformation.state.CloudFormationChangesProducer;
import ca.vanzyl.ck8s.state.EntityChangeProducer;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;

import javax.inject.Named;

@Named
public class CloudFormationTaskModule implements com.google.inject.Module {

    @Override
    public void configure (Binder binder) {
        var actions = Multibinder.newSetBinder(binder, CloudFormationTaskAction.class);
        actions.addBinding().to(CreateAction.class);
        actions.addBinding().to(CreateChangeSetAction.class);
        actions.addBinding().to(DeleteAction.class);
        actions.addBinding().to(DeletePreviewAction.class);
        actions.addBinding().to(DeployAction.class);
        actions.addBinding().to(ExecuteChangeSetAction.class);
        actions.addBinding().to(ExistsAction.class);
        actions.addBinding().to(GetTemplateAction.class);

        var changesProducers = Multibinder.newSetBinder(binder, EntityChangeProducer.class);
        changesProducers.addBinding().to(CloudFormationChangesProducer.class);
    }
}
