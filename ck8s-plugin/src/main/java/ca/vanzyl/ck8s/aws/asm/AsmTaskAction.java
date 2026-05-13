package ca.vanzyl.ck8s.aws.asm;

import ca.vanzyl.ck8s.actions.ActionName;
import ca.vanzyl.ck8s.actions.TaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public abstract class AsmTaskAction<T extends AsmTaskParams> implements TaskAction<T, AsmTaskAction.Action> {

    private final AsmClientFactory clientFactory;

    protected AsmTaskAction(AsmClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    protected SecretsManagerClient createClient(AsmTaskParams input) {
        return clientFactory.create(input.baseParams().profile(), input.baseParams().region());
    }

    @Override
    public abstract Action action();

    @Override
    public abstract TaskResult execute(Context context, T input) throws Exception;

    public enum Action implements ActionName {
        CREATE_SECRET("create-secret"),
        GET_SECRET("get-secret"),
        UPDATE_SECRET("update-secret"),
        DELETE_SECRET("delete-secret");

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