package ca.vanzyl.concord.k8s;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.walmartlabs.concord.server.sdk.process.CustomEnqueueProcessor;

import javax.inject.Named;

import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static com.walmartlabs.concord.server.Utils.bindJaxRsResource;

@Named
public class Ck8sModule implements Module {

    @Override
    public void configure(Binder binder) {
        bindJaxRsResource(binder, Ck8sProcessResourceV2.class);
        bindJaxRsResource(binder, Ck8sProcessResourceV3.class);
        bindJaxRsResource(binder, Ck8sClusterResourceV2.class);
        bindJaxRsResource(binder, ConcordExtResource.class);
        bindJaxRsResource(binder, Ck8sApiKeyResource.class);

        newSetBinder(binder, CustomEnqueueProcessor.class).addBinding().to(Ck8sRequestAttachmentProcessor.class);
        newSetBinder(binder, CustomEnqueueProcessor.class).addBinding().to(Ck8sArgumentsProcessor.class);
        newSetBinder(binder, CustomEnqueueProcessor.class).addBinding().to(Ck8sStateProcessor.class);
    }
}
