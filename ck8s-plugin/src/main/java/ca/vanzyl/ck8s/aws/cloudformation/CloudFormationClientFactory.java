package ca.vanzyl.ck8s.aws.cloudformation;

import ca.vanzyl.ck8s.aws.AwsUserAgent;
import ca.vanzyl.ck8s.aws.CredentialsProvider;
import com.walmartlabs.concord.runtime.common.injector.InstanceId;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.UUID;

@Named
public final class CloudFormationClientFactory {

    private final CredentialsProvider credentialsProvider;
    private final UUID processInstanceId;

    @Inject
    public CloudFormationClientFactory(CredentialsProvider credentialsProvider, InstanceId instanceId) {
        this.credentialsProvider = credentialsProvider;
        this.processInstanceId = instanceId.getValue();
    }

    public CloudFormationClient create(String profile, Region region) {
        return CloudFormationClient.builder()
                .region(region)
                .credentialsProvider(credentialsProvider.get(profile))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .putHeader("User-Agent", AwsUserAgent.build(processInstanceId, "ck8sAwsCloudFormation"))
                        .build())
                .build();
    }
}
