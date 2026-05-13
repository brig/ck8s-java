package ca.vanzyl.ck8s.aws.cognito;

import ca.vanzyl.ck8s.aws.cognito.actions.*;
import ca.vanzyl.ck8s.aws.cognito.state.*;
import ca.vanzyl.ck8s.state.EntityChangeProducer;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;

import javax.inject.Named;

@Named
public class CognitoTaskModule implements com.google.inject.Module {

    @Override
    public void configure (Binder binder) {
        var actions = Multibinder.newSetBinder(binder, CognitoTaskAction.class);
        actions.addBinding().to(DeleteUserPoolAction.class);
        actions.addBinding().to(DeleteUserPoolsAction.class);
        actions.addBinding().to(ListUserPoolsAction.class);
        actions.addBinding().to(FindUserPoolAction.class);
        actions.addBinding().to(FindUserPoolClientAction.class);
        actions.addBinding().to(VerifyUserPoolAction.class);
        actions.addBinding().to(UpsertUserPoolAction.class);
        actions.addBinding().to(GetUserPoolClientAction.class);
        actions.addBinding().to(UpsertIdentityProviderAction.class);
        actions.addBinding().to(VerifyIdentityProviderAction.class);
        actions.addBinding().to(UpsertResourceServerAction.class);
        actions.addBinding().to(VerifyResourceServerAction.class);
        actions.addBinding().to(UpsertUserPoolClientAction.class);
        actions.addBinding().to(VerifyUserPoolClientAction.class);
        actions.addBinding().to(UpsertUserPoolDomainAction.class);
        actions.addBinding().to(VerifyUserPoolDomainAction.class);
        actions.addBinding().to(UpsertUserPoolUICustomizationAction.class);
        actions.addBinding().to(VerifyUserPoolUICustomizationAction.class);
        actions.addBinding().to(CreateUserPoolUserAction.class);
        actions.addBinding().to(VerifyUserPoolUserAction.class);
        actions.addBinding().to(UpsertUserPoolPreviewAction.class);
        actions.addBinding().to(UpsertIdentityProviderPreviewAction.class);
        actions.addBinding().to(UpsertResourceServerPreviewAction.class);
        actions.addBinding().to(UpsertUserPoolClientPreviewAction.class);
        actions.addBinding().to(CreateUserPoolUserPreviewAction.class);
        actions.addBinding().to(UpsertUserPoolClientCallbacksAction.class);
        actions.addBinding().to(VerifyUserPoolClientCallbacksAction.class);

        actions.addBinding().to(VerifyUserPoolActionInAwsPermissionsPhase.class);
        actions.addBinding().to(VerifyIdentityProviderActionInAwsPermissionsPhase.class);
        actions.addBinding().to(VerifyResourceServerActionInAwsPermissionsPhase.class);
        actions.addBinding().to(VerifyUserPoolClientActionInAwsPermissionsPhase.class);
        actions.addBinding().to(VerifyUserPoolDomainActionInAwsPermissionsPhase.class);
        actions.addBinding().to(VerifyUserPoolUICustomizationActionInAwsPermissionsPhase.class);
        actions.addBinding().to(VerifyUserPoolUserActionInAwsPermissionsPhase.class);

        var changesProducers = Multibinder.newSetBinder(binder, EntityChangeProducer.class);
        changesProducers.addBinding().to(UserPoolChangesProducer.class);
        changesProducers.addBinding().to(IdentityProviderChangesProducer.class);
        changesProducers.addBinding().to(ResourceServerChangesProducer.class);
        changesProducers.addBinding().to(UserPoolClientChangesProducer.class);
        changesProducers.addBinding().to(UserPoolUserChangesProducer.class);
    }
}
