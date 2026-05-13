package ca.vanzyl.concord.k8s.model;

import org.immutables.value.Value;

import javax.annotation.Nullable;

public interface InfrastructureRequest
{

    @Value.Default
    default String organization()
    {
        return "myco";
    }

    @Value.Default
    default String provider()
    {
        return "aws";
    }

    @Value.Default
    default String region()
    {
        return "us-east-2";
    }

    @Nullable
    String account();

    @Nullable
    String user();
}
