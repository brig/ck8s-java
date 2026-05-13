package ca.vanzyl.ck8s.aws.iam.state;

import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.actions.CreatePolicyAction;
import software.amazon.awssdk.regions.Region;

import static ca.vanzyl.ck8s.aws.iam.actions.CreatePolicyOrVerifyAction.policyDocument;

public class IamManagedPolicyLoader extends AbstractIamEntityLoader<IamManagedPolicyKey, IamManagedPolicy> {

    public IamManagedPolicyLoader(IamClientFactory clientFactory, String profile, Region region) {
        super(clientFactory, profile, region);
    }

    @Override
    public IamManagedPolicy load(IamManagedPolicyKey key) {
        try (var client = createClient()) {
            var awsPolicy = CreatePolicyAction.getPolicy(client, key.policyArn());
            if (awsPolicy == null) {
                return null;
            }

            return new IamManagedPolicy(awsPolicy.arn(), awsPolicy.policyName(), policyDocument(client, key.policyArn(), awsPolicy.defaultVersionId()));
        }
    }
}
