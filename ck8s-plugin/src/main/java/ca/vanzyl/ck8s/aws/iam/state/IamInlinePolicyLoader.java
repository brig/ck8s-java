package ca.vanzyl.ck8s.aws.iam.state;

import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import software.amazon.awssdk.regions.Region;

import static ca.vanzyl.ck8s.aws.iam.actions.PutPolicyOrVerifyAction.inlinePolicyDocument;

public class IamInlinePolicyLoader extends AbstractIamEntityLoader<IamInlinePolicyKey, IamInlinePolicy> {

    public IamInlinePolicyLoader(Context context, IamClientFactory clientFactory, String profile, Region region) {
        super(context, clientFactory, profile, region);
    }

    @Override
    public IamInlinePolicy load(IamInlinePolicyKey key) {
        var roleName = key.roleName();
        var policyName = key.policyName();

        try (var client = createClient()) {
            var awsInlinePolicyDocument = inlinePolicyDocument(client, roleName, policyName);
            if (awsInlinePolicyDocument == null) {
                return null;
            }

            return new IamInlinePolicy(roleName, policyName, awsInlinePolicyDocument);
        }
    }
}
