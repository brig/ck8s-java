package ca.vanzyl.ck8s.aws.efs;

import ca.vanzyl.ck8s.actions.ActionName;
import ca.vanzyl.ck8s.actions.TaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import software.amazon.awssdk.services.efs.EfsClient;

public abstract class EfsTaskAction<T extends EfsTaskParams> implements TaskAction<T, EfsTaskAction.Action> {

    protected final EfsClientFactory clientFactory;

    public abstract Action action();

    public abstract TaskResult execute(Context context, T input) throws Exception;

    protected EfsTaskAction(EfsClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    protected EfsClient createClient(EfsTaskParams input) {
        return createClient(input.baseParams());
    }

    protected EfsClient createClient(EfsTaskParams.BaseParams params) {
        return clientFactory.create(params.profile(), params.region());
    }

    public enum Action implements ActionName {

        FIND_ACCESS_POINT("find-access-point"),
        CREATE_ACCESS_POINT("create-access-point"),
        DELETE_ACCESS_POINT("delete-access-point"),
        FIND_FILE_SYSTEM("find-file-system");

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
