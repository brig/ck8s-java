package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UICustomizationType;
import software.amazon.awssdk.services.glue.model.EntityNotFoundException;

import javax.inject.Inject;

public class UpsertUserPoolUICustomizationAction extends CognitoTaskAction<CognitoTaskParams.CreateUserPoolUICustomizationParams> {

    private static final Logger log = LoggerFactory.getLogger(UpsertUserPoolUICustomizationAction.class);

    @Inject
    public UpsertUserPoolUICustomizationAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.UPSERT_USER_POOL_UI_CUSTOMIZATION;
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateUserPoolUICustomizationParams input) throws Exception {
        var poolId = input.poolId();

        try (var client = createClient(input)) {
            var existingCustomization = getUiCustomization(client, poolId);
            if (existingCustomization == null) {
                log.info("User pool '{}' UI Customization does not exists. Creating it...", poolId);

                client.setUICustomization(r -> r.userPoolId(poolId)
                        .imageFile(SdkBytes.fromByteArray(input.image()))
                        .css(input.css()));

                log.info("✅ User pool '{}' UI Customization created", poolId);
            } else {
                log.info("User pool '{}' UI Customization exists. Updating it...", poolId);

                client.setUICustomization(r -> r.userPoolId(poolId)
                        .imageFile(SdkBytes.fromByteArray(input.image()))
                        .css(input.css()));

                log.info("✅ User pool '{}' UI Customization updated", poolId);
            }
            return TaskResult.success();
        } catch (CognitoIdentityProviderException e) {
            log.error("❌ Failed to upsert user pool UI customization '{}': {}", poolId, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }

    public static UICustomizationType getUiCustomization(CognitoIdentityProviderClient client, String poolId) {
        try {
            return client.getUICustomization(r -> r.userPoolId(poolId)).uiCustomization();
        } catch (EntityNotFoundException e) {
            return null;
        }
    }
}
