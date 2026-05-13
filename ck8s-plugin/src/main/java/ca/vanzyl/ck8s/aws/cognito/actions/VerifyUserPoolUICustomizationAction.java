package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;

import static ca.vanzyl.ck8s.utils.VerifyUtils.verifyAttribute;

public class VerifyUserPoolUICustomizationAction extends CognitoTaskAction<CognitoTaskParams.CreateUserPoolUICustomizationParams> {

    private static final Logger log = LoggerFactory.getLogger(VerifyUserPoolUICustomizationAction.class);

    @Inject
    public VerifyUserPoolUICustomizationAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.VERIFY_USER_POOL_UI_CUSTOMIZATION;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.CreateUserPoolUICustomizationParams input) throws Exception {
        var poolId = input.poolId();

        try (var client = createClient(input)) {
            var existingCustomization = UpsertUserPoolUICustomizationAction.getUiCustomization(client, poolId);
            if (existingCustomization == null) {
                log.error("❌ User Pool '{}' UI customization does not exists", poolId);
                return TaskResult.fail("User pool UI customization not found");
            }

            log.info("User pool '{}' UI Customization exists. Verifying it...", poolId);

            var valid = verifyAttribute("CSS", existingCustomization.css(), input.css());
            if (!valid) {
                return TaskResult.fail("User pool UI customization changed");
            }
            log.info("✅ User pool '{}' UI Customization unchanged", poolId);
            return TaskResult.success();
        }
    }
}
