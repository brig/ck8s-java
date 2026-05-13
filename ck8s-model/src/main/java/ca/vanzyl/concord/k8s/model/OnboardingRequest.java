package ca.vanzyl.concord.k8s.model;

import ca.vanzyl.concord.k8s.model.aws.Aws;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableOnboardingRequest.class)
@JsonDeserialize(as = ImmutableOnboardingRequest.class)
public interface OnboardingRequest
{

    static ImmutableOnboardingRequest.Builder builder()
    {
        return ImmutableOnboardingRequest.builder();
    }

    @ValidSubdomain
    String code();

    @Nullable
    String externalId();

    // TODO(ib): validation?
    @Nullable
    String accountNumber();

    @Value.Default
    default String provider()
    {
        if (aws() != null) {
            return "aws";
        }
        return "aws";
    }

    @Nullable
    Aws aws();
}
