package ca.vanzyl.ck8s.aws.glue;

import ca.vanzyl.ck8s.aws.AwsUserAgent;
import ca.vanzyl.ck8s.aws.CredentialsProvider;
import com.walmartlabs.concord.runtime.common.injector.InstanceId;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.glue.GlueClient;

import com.walmartlabs.concord.runtime.v2.sdk.Context;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Named;
import java.util.UUID;

@Named
public class GlueClientFactory {

    private final CredentialsProvider credentialsProvider;
    // resolved per call: the factory outlives a single task invocation
    private final Provider<Context> context;
    private final UUID processInstanceId;

    @Inject
    public GlueClientFactory(CredentialsProvider credentialsProvider, InstanceId instanceId, Provider<Context> context) {
        this.credentialsProvider = credentialsProvider;
        this.context = context;
        this.processInstanceId = instanceId.getValue();
    }

    public GlueClient create(String profile, Region region) {
        return GlueClient.builder()
                .region(region)
                .credentialsProvider(credentialsProvider.get(context.get(), profile))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .putHeader("User-Agent", AwsUserAgent.build(processInstanceId, "ck8sGlue"))
                        .build())
                .build();
    }
}
