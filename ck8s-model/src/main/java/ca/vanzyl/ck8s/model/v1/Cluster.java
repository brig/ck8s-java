package ca.vanzyl.ck8s.model.v1;

import ca.vanzyl.ck8s.model.v1.aws.Aws;
import ca.vanzyl.ck8s.model.v1.dns.Dns;
import ca.vanzyl.ck8s.model.v1.helm.Helm;
import ca.vanzyl.ck8s.model.v1.images.Images;
import ca.vanzyl.ck8s.model.v1.observability.Observability;
import ca.vanzyl.ck8s.model.v1.registry.Registry;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableCluster.class)
@JsonDeserialize(as = ImmutableCluster.class)
@JsonPropertyOrder({
        "version",
        // Organization with fallback to ck8s defaults
        "debug",
        "group",
        "profile",
        "k8sVersion",
        "domain",
        // Cluster specific
        "alias",
        "clusterName",
        "account",
        "accountId",
        "region",
        "regionShort",
        "environment",
        "helm",
        "images",
        "registry",
        "clusterLogging",
        "metricsDomain",
        "clusterLoggingBucket",
        "clusterLoggingDomain",
        "clusterLoggingGateway",
        "clusterLoggingGwWhitelist",
        "enableLokiGwForApps",
        "observability",
        "concordRoleArn",
        "oauthBaseURL",
        "provider",
        "aws"
})
public interface Cluster
{

    static ImmutableCluster.Builder builder()
    {
        return ImmutableCluster.builder();
    }

    String version();

    @Value.Default
    default boolean debug()
    {
        return false;
    }

    // Organization-level

    String group();

    String profile();

    String k8sVersion();

    String domain();

    // Cluster-level

    String alias();

    String clusterName();

    String account();

    String accountId();

    String region();

    String regionShort();

    String environment();

    String oauthBaseURL();

    Helm helm();

    Images images();

    Registry registry();

    // --------------------------------------------------------------------------
    // Observability
    // --------------------------------------------------------------------------

    // Observability properties that started out at the top-level but need to be
    // migrated into the Observability class

    // This originally was for cloudtrail logging in AWS
    boolean clusterLogging();

    String metricsDomain();

    String clusterLoggingBucket();

    String clusterLoggingDomain();

    String clusterLoggingGateway();

    String clusterLoggingGwWhitelist();

    List<String> enableLokiGwForApps();

    @Nullable
    Observability observability();

    // --------------------------------------------------------------------------
    // DNS Provider
    // --------------------------------------------------------------------------

    @Nullable
    Dns dns();

    // --------------------------------------------------------------------------
    // Cloud Provider
    // --------------------------------------------------------------------------

    String provider();

    @Nullable
    Aws aws();

    String concordRoleArn();
}
