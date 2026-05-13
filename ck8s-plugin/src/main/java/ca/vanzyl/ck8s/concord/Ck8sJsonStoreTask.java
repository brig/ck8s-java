package ca.vanzyl.ck8s.concord;

import ca.vanzyl.ck8s.common.MergeUtils;
import com.walmartlabs.concord.client2.ApiClient;
import com.walmartlabs.concord.client2.ApiException;
import com.walmartlabs.concord.client2.ClientUtils;
import com.walmartlabs.concord.client2.JsonStoreDataApi;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.DryRunReady;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

@Named("ck8sJsonStore")
@DryRunReady
public class Ck8sJsonStoreTask implements Task {

    private static final int RETRY_COUNT = 5;
    private static final long RETRY_INTERVAL = 15000;

    private static final Logger log = LoggerFactory.getLogger(Ck8sJsonStoreTask.class);

    private final JsonStoreDataApi client;
    private final boolean dryRunMode;
    private final String processOrg;

    @Inject
    public Ck8sJsonStoreTask(Context context, ApiClient apiClient) {
        this.client = new JsonStoreDataApi(apiClient);
        this.dryRunMode = context.processConfiguration().dryRun();
        this.processOrg = context.processConfiguration().projectInfo().orgName();
    }

    public void merge(String storeName, String itemPath, Map<String, Object> data) throws ApiException {
        merge(assertOrg(processOrg), storeName, itemPath, data);
    }

    @SuppressWarnings("unchecked")
    public void merge(String orgName, String storeName, String itemPath, Map<String, Object> data) throws ApiException{
        var prevData = ClientUtils.withRetry(RETRY_COUNT, RETRY_INTERVAL, () -> client.getJsonStoreData(orgName, storeName, itemPath));

        if (prevData == null) {
            prevData = Map.of();
        }

        if (!(prevData instanceof Map)) {
            throw new UserDefinedException("The previous data is not a Map: " + prevData);
        }

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping merging item '{}'/'{}'", storeName, itemPath);
            return;
        }

        var newData = MergeUtils.merge((Map<String, Object>) prevData, data);

        log.info("Merging item '{}/{}/{}'", orgName, storeName, itemPath);

        ClientUtils.withRetry(RETRY_COUNT, RETRY_INTERVAL, () -> client.updateJsonStoreData(orgName, storeName, itemPath, newData));
    }

    private static String assertOrg(String org) {
        if (org != null) {
            return org;
        }

        throw new RuntimeException("Can't determine the current organization name. " +
                "Please specify it explicitly or run your process in a project.");
    }
}
