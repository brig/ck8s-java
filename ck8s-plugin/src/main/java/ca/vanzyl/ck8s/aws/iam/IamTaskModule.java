package ca.vanzyl.ck8s.aws.iam;

import ca.vanzyl.ck8s.aws.iam.actions.*;
import ca.vanzyl.ck8s.aws.iam.state.IamInlinePolicyChangesProducer;
import ca.vanzyl.ck8s.aws.iam.state.IamManagedPolicyChangesProducer;
import ca.vanzyl.ck8s.aws.iam.state.IamRoleChangesProducer;
import ca.vanzyl.ck8s.state.EntityChangeProducer;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;

import javax.inject.Named;

@Named
public class IamTaskModule implements com.google.inject.Module {

    @Override
    public void configure (Binder binder) {
        var actions = Multibinder.newSetBinder(binder, IamTaskAction.class);
        actions.addBinding().to(VerifyRoleAction.class);
        actions.addBinding().to(VerifyRoleActionInAwsPermissionsPhase.class);

        actions.addBinding().to(CreateRoleAction.class);
        actions.addBinding().to(CreateRolePreviewAction.class);
        actions.addBinding().to(CreateRoleOrVerifyAction.class);
        actions.addBinding().to(CreateRoleOrVerifyPreviewAction.class);
        actions.addBinding().to(DeleteRoleAction.class);
        actions.addBinding().to(DeleteRolePreviewAction.class);
        actions.addBinding().to(GetRoleAction.class);
        actions.addBinding().to(ListRolesAction.class);

        actions.addBinding().to(VerifyInlinePolicyAction.class);
        actions.addBinding().to(VerifyInlinePolicyInAwsPermissionsPhase.class);

        actions.addBinding().to(PutPolicyAction.class);
        actions.addBinding().to(PutPolicyPreviewAction.class);
        actions.addBinding().to(PutPolicyOrVerifyAction.class);
        actions.addBinding().to(PutPolicyOrVerifyPreviewAction.class);

        actions.addBinding().to(CreatePolicyAction.class);
        actions.addBinding().to(CreatePolicyPreviewAction.class);
        actions.addBinding().to(CreatePolicyOrVerifyAction.class);
        actions.addBinding().to(CreatePolicyOrVerifyPreviewAction.class);
        actions.addBinding().to(DeletePolicyAction.class);
        actions.addBinding().to(DeletePolicyPreviewAction.class);
        actions.addBinding().to(VerifyPolicyAction.class);

        actions.addBinding().to(AttachPolicyAction.class);
        actions.addBinding().to(AttachPolicyVerifyAction.class);
        actions.addBinding().to(AttachPolicyPreviewAction.class);
        actions.addBinding().to(ListPoliciesAction.class);

        actions.addBinding().to(VerifyPolicyActionInAwsPermissionsPhase.class);
        actions.addBinding().to(AttachPolicyVerifyActionInAwsPermissionsPhase.class);

        var changesProducers = Multibinder.newSetBinder(binder, EntityChangeProducer.class);
        changesProducers.addBinding().to(IamRoleChangesProducer.class);
        changesProducers.addBinding().to(IamManagedPolicyChangesProducer.class);
        changesProducers.addBinding().to(IamInlinePolicyChangesProducer.class);
    }
}
