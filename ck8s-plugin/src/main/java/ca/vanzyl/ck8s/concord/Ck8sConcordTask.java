package ca.vanzyl.ck8s.concord;

import com.walmartlabs.concord.client2.ApiClient;
import com.walmartlabs.concord.client2.ApiException;
import com.walmartlabs.concord.client2.ClientUtils;
import com.walmartlabs.concord.client2.ProcessApi;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Named("ck8sConcord")
public class Ck8sConcordTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(Ck8sConcordTask.class);

    private final ProcessApi processApi;
    private final Context context;

    @Inject
    public Ck8sConcordTask(ApiClient apiClient, Context context) {
        this.processApi = new ProcessApi(apiClient);
        this.context = context;
    }

    public void resume(String instanceId, String eventName, String saveAs, Map<String, Object> payload) throws ApiException {
        ClientUtils.withRetry(3, 10000, () -> {
            processApi.resume(UUID.fromString(instanceId), eventName, saveAs, payload);
            return null;
        });
    }

    public void suspend(String eventId) {
        this.context.suspend(eventId);
    }

    public void downloadAttachments(String instanceId, List<String> attachments) {
        for (var attachment : attachments) {
            try (var in = processApi.downloadAttachment(UUID.fromString(instanceId), attachment)) {
                var dest = Path.of(attachment);
                Files.createDirectories(dest.getParent());
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                log.error("Error downloading attachment {}", attachment, e);
                throw new RuntimeException(e.getMessage());
            }
        }
    }
}
