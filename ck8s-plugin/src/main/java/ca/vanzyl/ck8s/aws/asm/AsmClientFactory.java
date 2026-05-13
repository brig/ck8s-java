package ca.vanzyl.ck8s.aws.asm;

import ca.vanzyl.ck8s.aws.AwsUserAgent;
import ca.vanzyl.ck8s.aws.CredentialsProvider;
import com.walmartlabs.concord.runtime.common.injector.InstanceId;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.UUID;

@Named
public final class AsmClientFactory {

    private final CredentialsProvider credentialsProvider;
    private final UUID processInstanceId;

    @Inject
    public AsmClientFactory(CredentialsProvider credentialsProvider, InstanceId instanceId) {
        this.credentialsProvider = credentialsProvider;
        this.processInstanceId = instanceId.getValue();
    }

    public SecretsManagerClient create(String profile, Region region) {
        return SecretsManagerClient.builder()
                .region(region)
                .credentialsProvider(credentialsProvider.get(profile))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .putHeader("User-Agent", AwsUserAgent.build(processInstanceId, "ck8sAwsAsm"))
                        .build())
                .build();
    }
}