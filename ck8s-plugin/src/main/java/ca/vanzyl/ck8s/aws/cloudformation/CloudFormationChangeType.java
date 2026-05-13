package ca.vanzyl.ck8s.aws.cloudformation;

public final class CloudFormationChangeType {

    public static final String CLOUD_FORMATION_TYPE = "aws:cloudformation";

    public static String cloudFormationId(String region, String stackName) {
        return String.format("aws:%s:cloudformation:%s", region, stackName);
    }

    private CloudFormationChangeType() {
    }
}
