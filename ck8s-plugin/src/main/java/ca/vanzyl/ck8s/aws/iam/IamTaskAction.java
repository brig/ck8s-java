package ca.vanzyl.ck8s.aws.iam;

import ca.vanzyl.ck8s.actions.ActionName;
import ca.vanzyl.ck8s.actions.TaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import software.amazon.awssdk.services.iam.IamClient;

public abstract class IamTaskAction<T extends IamTaskParams> implements TaskAction<T, IamTaskAction.Action> {

    protected final IamClientFactory clientFactory;

    public abstract Action action();

    public abstract TaskResult execute(Context context, T input) throws Exception;

    protected IamTaskAction(IamClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    protected IamClient createClient(IamTaskParams input) {
        return createClient(input.baseParams());
    }

    protected IamClient createClient(IamTaskParams.BaseParams params) {
        return clientFactory.create(params.profile(), params.region());
    }

    public enum Action implements ActionName {

        CREATE_ROLE("create-role"),
        VERIFY_ROLE("verify-role"),
        GET_ROLE("get-role"),
        DELETE_ROLE("delete-role"),
        LIST_ROLES("list-roles"),

        CREATE_ROLE_OR_VERIFY("create-role-or-verify"),

        PUT_ROLE_POLICY("put-role-policy"),
//        CREATE_INLINE_POLICY("create-inline-policy"),
        VERIFY_INLINE_POLICY("verify-inline-policy"),

        PUT_ROLE_POLICY_OR_VERIFY("put-role-policy-or-verify"),

        CREATE_POLICY("create-policy"),
        VERIFY_POLICY("verify-policy"),

        CREATE_POLICY_OR_VERIFY("create-policy-or-verify"),
        DELETE_POLICY("delete-policy"),
        LIST_POLICIES("list-policies"),
        ATTACH_POLICY("attach-policy"),
        ATTACH_POLICY_VERIFY("attach-policy-verify");

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
