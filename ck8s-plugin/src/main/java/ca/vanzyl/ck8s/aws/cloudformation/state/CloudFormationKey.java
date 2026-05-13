package ca.vanzyl.ck8s.aws.cloudformation.state;

import ca.vanzyl.ck8s.state.EntityKey;

public record CloudFormationKey (String region, String stackName) implements EntityKey<CloudFormationEntity> {

    @Override
    public Class<CloudFormationEntity> entityType() {
        return CloudFormationEntity.class;
    }
}
