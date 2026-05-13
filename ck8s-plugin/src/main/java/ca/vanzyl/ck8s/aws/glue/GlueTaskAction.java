package ca.vanzyl.ck8s.aws.glue;

import ca.vanzyl.ck8s.actions.ActionName;
import ca.vanzyl.ck8s.actions.TaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import software.amazon.awssdk.services.glue.GlueClient;

public abstract class GlueTaskAction<T extends GlueTaskParams> implements TaskAction<T, GlueTaskAction.Action> {

    protected final GlueClientFactory clientFactory;

    public abstract Action action();

    public abstract TaskResult execute(Context context, T input) throws Exception;

    protected GlueTaskAction(GlueClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    protected GlueClient createClient(GlueTaskParams input) {
        return createClient(input.baseParams());
    }

    protected GlueClient createClient(GlueTaskParams.BaseParams params) {
        return clientFactory.create(params.profile(), params.region());
    }

    public enum Action implements ActionName {

        EXISTS("exists");

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
