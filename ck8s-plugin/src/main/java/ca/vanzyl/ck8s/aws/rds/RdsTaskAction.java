package ca.vanzyl.ck8s.aws.rds;

import ca.vanzyl.ck8s.actions.ActionName;
import ca.vanzyl.ck8s.actions.TaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import software.amazon.awssdk.services.rds.RdsClient;

public abstract class RdsTaskAction<T extends RdsTaskParams> implements TaskAction<T, RdsTaskAction.Action> {

    protected final RdsClientFactory clientFactory;

    public abstract Action action();

    public abstract TaskResult execute(Context context, T input) throws Exception;

    protected RdsTaskAction(RdsClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    protected RdsClient createClient(RdsTaskParams input) {
        return createClient(input.baseParams());
    }

    protected RdsClient createClient(RdsTaskParams.BaseParams params) {
        return clientFactory.create(params.profile(), params.region());
    }

    public enum Action implements ActionName {

        FETCH_ENDPOINT("fetch-endpoint");

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
