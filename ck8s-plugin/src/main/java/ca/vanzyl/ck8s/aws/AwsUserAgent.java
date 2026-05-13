package ca.vanzyl.ck8s.aws;

import com.walmartlabs.concord.runtime.v2.sdk.Context;

import java.util.UUID;

public final class AwsUserAgent {

    @Deprecated
    public static String build(Context context, String taskName) {
        return build(context.processInstanceId(), taskName);
    }

    public static String build(UUID instanceId, String taskName) {
        return taskName + "/Concord/" + instanceId;
    }

    private AwsUserAgent() {
    }
}
