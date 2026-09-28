package ca.vanzyl.ck8s.aws.efs;

import ca.vanzyl.ck8s.aws.AwsUserAgent;
import ca.vanzyl.ck8s.aws.CredentialsProvider;
import com.walmartlabs.concord.runtime.common.injector.InstanceId;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.efs.EfsClient;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.UUID;

@Named
public class EfsClientFactory {

    private final CredentialsProvider credentialsProvider;
    private final UUID processInstanceId;

    @Inject
    public EfsClientFactory(CredentialsProvider credentialsProvider, InstanceId instanceId) {
        this.credentialsProvider = credentialsProvider;
        this.processInstanceId = instanceId.getValue();
    }

    public EfsClient create(Context context, String profile, Region region) {
        return EfsClient.builder()
                .region(region)
                .credentialsProvider(credentialsProvider.get(context, profile))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .putHeader("User-Agent", AwsUserAgent.build(processInstanceId, "ck8sEfs"))
                        .build())
                .build();
    }
}
