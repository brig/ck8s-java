package ca.vanzyl.concord.k8s.model.applications;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableHiveApplicationDeployment.class)
@JsonDeserialize(as = ImmutableHiveApplicationDeployment.class)
public interface HiveApplicationDeployment
        extends K8sApplicationDeployment
{

    static ImmutableHiveApplicationDeployment.Builder builder()
    {
        return ImmutableHiveApplicationDeployment.builder();
    }

    @Override
    @Value.Default
    default String type()
    {
        return K8sApplicationDeployment.APPLICATION_HIVE_TYPE;
    }

    @Value.Default
    default String cpu()
    {
        return "0.5";
    }

    @Value.Default
    default String memory()
    {
        return "1Gi";
    }

    @Value.Default
    default S3Settings s3()
    {
        return S3Settings.builder().build();
    }
}
