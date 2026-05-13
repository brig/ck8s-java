package ca.vanzyl.ck8s.aws.cognito;

import ca.vanzyl.ck8s.actions.ActionName;
import ca.vanzyl.ck8s.actions.TaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

public abstract class CognitoTaskAction<T extends CognitoTaskParams> implements TaskAction<T, CognitoTaskAction.Action> {

    private final CognitoClientFactory clientFactory;

    protected CognitoTaskAction(CognitoClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    protected CognitoIdentityProviderClient createClient(CognitoTaskParams input) {
        return clientFactory.create(input.baseParams().profile(), input.baseParams().region());
    }

    @Override
    public abstract TaskResult execute(Context context, T input) throws Exception;

    @Override
    public abstract Action action();

    public enum Action implements ActionName {
        UPSERT_USER_POOL("upsert-user-pool"),
        VERIFY_USER_POOL("verify-user-pool"),

        LIST_USER_POOLS ("list-user-pools"),
        FIND_USER_POOL("find-user-pool"),

        GET_USER_POOL_CLIENT("get-user-pool-client"),
        FIND_USER_POOL_CLIENT("find-user-pool-client"),

        DELETE_USER_POOLS ("delete-user-pools"),
        DELETE_USER_POOL ("delete-user-pool"),

        UPSERT_IDENTITY_PROVIDER ("upsert-identity-provider"),
        VERIFY_IDENTITY_PROVIDER ("verify-identity-provider"),

        UPSERT_RESOURCE_SERVER("upsert-resource-server"),
        VERIFY_RESOURCE_SERVER ("verify-resource-server"),

        UPSERT_USER_POOL_CLIENT("upsert-user-pool-client"),
        VERIFY_USER_POOL_CLIENT("verify-user-pool-client"),

        UPSERT_USER_POOL_DOMAIN("upsert-user-pool-domain"),
        VERIFY_USER_POOL_DOMAIN("verify-user-pool-domain"),

        UPSERT_USER_POOL_UI_CUSTOMIZATION("upsert-user-pool-ui-customization"),
        VERIFY_USER_POOL_UI_CUSTOMIZATION("verify-user-pool-ui-customization"),

        CREATE_USER_POOL_USER("create-user-pool-user"),
        VERIFY_USER_POOL_USER("verify-user-pool-user"),

        UPSERT_USER_POOL_CLIENT_CALLBACKS("ensure-user-pool-client-callbacks"),
        VERIFY_USER_POOL_CLIENT_CALLBACKS("verify-user-pool-client-callbacks");

        private final String value;

        Action(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
