package ca.vanzyl.concord.k8s.model.aws;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

import java.util.Map;

@Value.Immutable
@JsonSerialize(as = ImmutableAws.class)
@JsonDeserialize(as = ImmutableAws.class)
@JsonPropertyOrder({
        "region",
        "accessKeyId",
        "secretAccessKey",
        "instanceType",
        "cidr",
        "subnetBits",
        "peeredVpcs",
        "hostedZoneId"
})
public interface Aws
{

    static ImmutableAws.Builder builder()
    {
        return ImmutableAws.builder();
    }

    @Nullable
    String region();

    /**
     * Type of the authentication mechanism. Allowed values:
     * <ul>
     *     <li>credentials</li>
     *     <li>assume-role</li>
     * </ul>
     */
    @Nullable
    @Value.Default
    default String authentication()
    {
        return "credentials";
    }

    @Nullable
    String roleArn();

    @Nullable
    String externalId();

    /**
     * Instead of passing {@link #accessKeyId()} and {@link #secretAccessKey()}, the {@link #secretRef()} parameter can be used to specify the prefix of existing Concord secrets.
     * <p/>
     * The provisioning will look for {@code ${secretRef}-awsAccessKey} and {@code ${secretRef}-awsSecretKey} single value secrets.
     */
    @Nullable
    String secretRef();

    @Nullable
    String accessKeyId();

    @Nullable
    String secretAccessKey();

    @Nullable
    String sessionToken();

    @Value.Default
    default String instanceType()
    {
        return "m5.xlarge";
    }

    /**
     * The CIDR for the network that will be created for the Kubernetes cluster. Examples of provided values might be:
     * <p>
     * 10.1.0.0/24 172.1.0.0/24
     * <p>
     * If this value is null, a free CIDR will be found in the region specified in the request.
     */
    @Nullable
    String cidr();

    @Value.Default
    default int subnetBits()
    {
        return 6;
    }

    @Value.Default
    default PeeredVpcs[] peeredVpcs()
    {
        return new PeeredVpcs[0];
    }

    @Nullable
    String hostedZoneId();

    @Nullable
    Map<String, String> tags();
}
