package ca.vanzyl.ck8s.aws.s3;

import ca.vanzyl.ck8s.actions.ActionName;
import ca.vanzyl.ck8s.actions.TaskAction;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import software.amazon.awssdk.services.s3.S3Client;

public abstract class S3TaskAction <T extends S3TaskParams> implements TaskAction<T, S3TaskAction.Action> {

    protected final S3ClientFactory clientFactory;

    public abstract Action action();

    public abstract TaskResult execute(Context context, T input) throws Exception;

    protected S3TaskAction(S3ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    protected S3Client createClient(S3TaskParams input) {
        return createClient(input.baseParams());
    }

    protected S3Client createClient(S3TaskParams.BaseParams params) {
        return clientFactory.create(params.profile(), params.region());
    }

    public enum Action implements ActionName {

        CREATE_BUCKET("create-bucket"),
        VERIFY_BUCKET("verify-bucket"),
        TAG_BUCKET("tag-bucket"),
        VERIFY_BUCKET_TAGS("verify-bucket-tags"),
        TAG_BUCKET_DEPRECATED_ACTION("tag");

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
