package ca.vanzyl.ck8s.aws;

import ca.vanzyl.ck8s.asserts.AssertsTask;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eks.EksClient;
import software.amazon.awssdk.services.eks.model.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static ca.vanzyl.ck8s.aws.AwsTaskUtils.assertRegion;

@Named("ck8sAwsEks")
@DryRunReady
public class EksTask implements Task {

    private final static Logger log = LoggerFactory.getLogger(EksTask.class);

    private final CredentialsProvider credentialsProvider;
    private final Context context;
    private final boolean dryRunMode;

    @Inject
    public EksTask(CredentialsProvider credentialsProvider, Context context) {
        this.credentialsProvider = credentialsProvider;
        this.context = context;
        this.dryRunMode = context.processConfiguration().dryRun();
    }

    @Override
    public TaskResult execute(Variables input) throws Exception {
        var action = input.assertString("action");
        if ("describe-cluster".equals(action)) {
            return describeCluster(input);
        } else if ("create-access-entry".equals(action)) {
            return createAccessEntry(input);
        }
        throw new IllegalArgumentException("Unsupported action: " + action);
    }

    private TaskResult createAccessEntry(Variables input) {
        var cluster = input.assertString("cluster");
        var roleArn = input.assertString("roleArn");
        var k8sClusterGroup = input.assertString("k8sClusterGroup");
        var username = input.assertString("username");

        try (var client = createClient(input)) {

            if (dryRunMode) {
                log.info("Dry-run mode enabled: Skipping creating IAM identity mapping");
                return TaskResult.success();
            }

            client.createAccessEntry(CreateAccessEntryRequest.builder()
                    .clusterName(cluster)
                    .principalArn(roleArn)
                    .type("ROLE")
                    .kubernetesGroups(k8sClusterGroup)
                    .username(username)
                    .build());

            log.info("✅ IAM Role '{}' for cluster '{}' successfully mapped to Kubernetes RBAC", roleArn, cluster);

            return TaskResult.success();
        } catch (EksException e) {
            throw new RuntimeException("Error creating IAM identity mapping: " + e.getMessage());
        }
    }

    public boolean isClusterAvailable(String region, String name) {
        AssertsTask.assertNotEmpty("region is empty", region);
        AssertsTask.assertNotEmpty("name is empty", name);

        try {
            describeCluster(Region.of(region), name);
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    private TaskResult describeCluster(Variables input) {
        var name =  input.assertString("name");
        var cluster = describeCluster(assertRegion(input), name);
        return TaskResult.success()
                .value("cluster", AwsTaskUtils.serialize(cluster));
    }

    private Cluster describeCluster(Region region, String name) {
        try (var client = createClient(new MapBackedVariables(Map.of("region", region.id())))) {
            var response = client.describeCluster(DescribeClusterRequest.builder()
                    .name(name)
                    .build());
            return response.cluster();
        }
    }

    private EksClient createClient(Variables input) {
        return EksClient.builder()
                .region(assertRegion(context, input))
                .credentialsProvider(credentialsProvider.get(context, input))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .putHeader("User-Agent", AwsUserAgent.build(context, "ck8sAwsCloudFormation"))
                        .build())
                .build();
    }
}
