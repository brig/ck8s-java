package ca.vanzyl.ck8s.aws.cloudformation;

import ca.vanzyl.ck8s.actions.ActionName;
import ca.vanzyl.ck8s.actions.TaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

public abstract class CloudFormationTaskAction<T extends CloudFormationTaskParams> implements TaskAction<T, CloudFormationTaskAction.Action> {

    private final CloudFormationClientFactory clientFactory;

    protected CloudFormationTaskAction(CloudFormationClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    protected CloudFormationClient createClient(CloudFormationTaskParams input) {
        return clientFactory.create(input.baseParams().profile(), input.baseParams().region());
    }

    @Override
    public abstract TaskResult execute(Context context, T input) throws Exception;

    @Override
    public abstract Action action();

    public enum Action implements ActionName {

        CREATE ("create"),
        CREATE_CHANGE_SET ("create-change-set"),
        DELETE ("delete"),
        DEPLOY ("deploy"),
        EXECUTE_CHANGE_SET ("execute-change-set"),
        EXISTS ("exists"),
        GET_TEMPLATE ("get-template");

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
