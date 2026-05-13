package ca.vanzyl.ck8s.aws;

import software.amazon.awssdk.regions.Region;

import java.io.Serial;
import java.io.Serializable;

public record StsAssumeRole(String profile, String region, String roleArn, String sessionName) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static StsAssumeRole from(String profile, Region region, String roleArn, String roleSessionName) {
        return new StsAssumeRole(profile, region.id(), roleArn, roleSessionName);
    }
}
