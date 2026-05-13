package ca.vanzyl.ck8s.aws.cloudformation.state;

import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationClientFactory;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTaskParams;
import ca.vanzyl.ck8s.state.EntityState;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CloudFormationState {

    private final CloudFormationClientFactory clientFactory;
    private final EntityState state;

    @Inject
    public CloudFormationState(EntityState state, CloudFormationClientFactory clientFactory) {
        this.state = state;
        this.clientFactory = clientFactory;
    }

    public CloudFormationEntity stack(CloudFormationTaskParams.BaseParams baseParams, String stackName) {
        return state.getOrLoad(new CloudFormationKey(baseParams.region().id(), stackName),
                new CloudFormationLoader(clientFactory, baseParams.profile(), baseParams.region()));
    }

    public void deleteStack(String region, String stackName) {
        state.delete(new CloudFormationKey(region, stackName));
    }
}
