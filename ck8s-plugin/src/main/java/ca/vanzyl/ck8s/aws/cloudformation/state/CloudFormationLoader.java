package ca.vanzyl.ck8s.aws.cloudformation.state;

import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationClientFactory;
import software.amazon.awssdk.regions.Region;

import static ca.vanzyl.ck8s.aws.cloudformation.CloudFormationUtils.stackExists;

public class CloudFormationLoader extends AbstractCloudFormationEntityLoader<CloudFormationKey, CloudFormationEntity> {

    public CloudFormationLoader(CloudFormationClientFactory clientFactory, String profile, Region region) {
        super(clientFactory, profile, region);
    }

    @Override
    public CloudFormationEntity load(CloudFormationKey key) {
        try (var client = createClient()) {
            var exists = stackExists(client, key.stackName());
            if (!exists) {
                return null;
            }

            return CloudFormationEntity.builder()
                    .stackName(key.stackName())
                    .build();
        }
    }
}
