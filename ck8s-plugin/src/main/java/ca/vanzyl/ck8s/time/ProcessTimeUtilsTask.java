package ca.vanzyl.ck8s.time;

import com.walmartlabs.concord.client2.ApiClient;
import com.walmartlabs.concord.client2.ProcessEntry;
import com.walmartlabs.concord.client2.ProcessV2Api;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Task;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;

@Named("processTimeUtils")
public class ProcessTimeUtilsTask
        implements Task {

    private final Context context;
    private final ProcessV2Api processApi;

    @Inject
    public ProcessTimeUtilsTask(Context context, ApiClient apiClient) {
        this.context = context;
        this.processApi = new ProcessV2Api(apiClient);
    }

    public Duration elapsedDuration() throws Exception {
        ProcessEntry entry = processApi.getProcess(context.processInstanceId(), Set.of());
        OffsetDateTime start = entry.getCreatedAt();
        OffsetDateTime end = entry.getLastUpdatedAt();
        return ProcessTimeUtils.elapsedDuration(start, end);
    }

    public String elapsedTime() throws Exception {
        ProcessEntry entry = processApi.getProcess(context.processInstanceId(), Set.of());
        OffsetDateTime start = entry.getCreatedAt();
        OffsetDateTime end = entry.getLastUpdatedAt();
        return ProcessTimeUtils.elapsedTime(start, end);
    }
}
