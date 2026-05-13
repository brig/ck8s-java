package ca.vanzyl.ck8s.aws.s3;

import ca.vanzyl.ck8s.aws.s3.actions.*;
import ca.vanzyl.ck8s.aws.s3.state.S3BucketChangesProducer;
import ca.vanzyl.ck8s.state.EntityChangeProducer;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;

import javax.inject.Named;

@Named
public class S3TaskModule implements com.google.inject.Module {

    @Override
    public void configure (Binder binder) {
        var actions = Multibinder.newSetBinder(binder, S3TaskAction.class);
        actions.addBinding().to(CreateBucketAction.class);
        actions.addBinding().to(CreateBucketPreviewAction.class);
        actions.addBinding().to(VerifyBucketAction.class);
        actions.addBinding().to(VerifyBucketTagsAction.class);

        actions.addBinding().to(TagBucketDeprecatedAction.class);
        actions.addBinding().to(TagBucketAction.class);
        actions.addBinding().to(TagBucketPreviewAction.class);

        actions.addBinding().to(VerifyBucketActionInAwsPermissionsPhase.class);
        actions.addBinding().to(VerifyBucketTagsActionInAwsPermissionsPhase.class);

        var changesProducers = Multibinder.newSetBinder(binder, EntityChangeProducer.class);
        changesProducers.addBinding().to(S3BucketChangesProducer.class);
    }
}
