package ca.vanzyl.ck8s.inventory;

import ca.vanzyl.ck8s.common.MapUtils;
import ca.vanzyl.ck8s.common.MergeUtils;
import com.walmartlabs.concord.client2.*;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Named("ck8sInventory")
@DryRunReady
public class Ck8sInventoryTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(Ck8sInventoryTask.class);

    private static final int RETRY_COUNT = 5;
    private static final long RETRY_INTERVAL = 15000;

    private final ApiClientFactory apiClientFactory;
    private final ApiClient defaultApiClient;
    private final Context context;
    private final boolean dryRunMode;

    @Inject
    public Ck8sInventoryTask(ApiClientFactory apiClientFactory, Context context, ApiClient apiClient) {
        this.apiClientFactory = apiClientFactory;
        this.defaultApiClient = apiClient;
        this.context = context;
        this.dryRunMode = context.processConfiguration().dryRun();
    }

    public List<Object> executeQuery(String storeName, String queryName) throws ApiException {
        return executeQuery(storeName, queryName, null);
    }


    public List<Object> executeQuery(String storeName, String queryName, Map<String, Object> params) throws ApiException {
        var orgName = orgName(context.defaultVariables());
        return executeQuery(orgName, storeName, queryName, params);
    }

    public List<Object> executeQuery(String orgName, String storeName, String queryName, Map<String, Object> params) throws ApiException {

        log.info("Executing query '{}/{}/{}' with parameters '{}'", orgName, storeName, queryName, params);

        return ClientUtils.withRetry(RETRY_COUNT, RETRY_INTERVAL, () -> {
            var api = new JsonStoreQueryApi(apiClient(defaultVariables(Map.of())));
            return api.execJsonStoreQuery(orgName, storeName, queryName, params);
        });
    }

    public void saveAppInfo(String namespace, String appName, Map<String, Object> appInfo) throws ApiException {
        assertNotEmpty("Namespace", namespace);
        assertNotEmpty("App name", appName);

        var orgName = orgName(context.defaultVariables());
        var storeName = storeName(context.defaultVariables());
        var itemPath = itemPath(namespace);

        var data = defaultInfo(namespace, appName);
        data.putAll(appInfo);

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping updating item '{}'/'{}'", storeName, itemPath);
            return;
        }

        log.info("Updating item '{}/{}/{}'", orgName, storeName, itemPath);

        ClientUtils.withRetry(RETRY_COUNT, RETRY_INTERVAL, () -> {
            var api = new JsonStoreDataApi(apiClient(defaultVariables(Map.of())));
            return api.updateJsonStoreData(orgName, storeName, itemPath, data);
        });
    }

    public void touchAppInfo(String namespace, String appName, Map<String, Object> appInfo) throws ApiException {
        assertNotEmpty("Namespace", namespace);
        assertNotEmpty("App name", appName);

        var orgName = orgName(context.defaultVariables());
        var storeName = storeName(context.defaultVariables());
        var itemPath = itemPath(namespace);

        var data = defaultInfo(namespace, appName);
        if (appInfo != null) {
            data.putAll(appInfo);
        }

        log.info("Touching item '{}/{}/{}'", orgName, storeName, itemPath);

        updateAppInfo(namespace, appName, data);
    }

    public void updateAppInfo(String namespace, String appName, Map<String, Object> appInfo) throws ApiException {
        assertNotEmpty("Namespace", namespace);
        assertNotEmpty("App name", appName);

        var prev = getAppInfo(clusterAlias(), namespace);
        saveAppInfo(namespace, appName, MergeUtils.merge(prev, appInfo));
    }

    public void removeFromAppInfo(String namespace, String appName, String key) throws ApiException {
        assertNotEmpty("Namespace", namespace);
        assertNotEmpty("App name", appName);

        var current = getAppInfo(clusterAlias(), namespace);
        var result = current.remove(key);
        if (result == null) {
            return;
        }

        var storeName = storeName(context.defaultVariables());
        var itemPath = itemPath(namespace);

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping removing app info item '{}'/'{}'", storeName, itemPath);
            return;
        }

        log.info("Removing '{}' from app info item '{}/{}/{}'",
                key, orgName(context.defaultVariables()), storeName, itemPath);

        saveAppInfo(namespace, appName, current);
    }

    public boolean deleteAppInfo(String namespace) throws ApiException {
        assertNotEmpty("Namespace", namespace);

        var orgName = orgName(context.defaultVariables());
        var storeName = storeName(context.defaultVariables());
        var itemPath = itemPath(namespace);

        if (dryRunMode) {
            log.info("Dry-run mode enabled: Skipping deleting app info item '{}'/'{}'", storeName, itemPath);
            return true;
        }

        log.info("Removing item '{}/{}/{}'", orgName, storeName, itemPath);

        return ClientUtils.withRetry(RETRY_COUNT, RETRY_INTERVAL, () -> {
            var api = new JsonStoreDataApi(apiClient(defaultVariables(Map.of())));
            var result = api.deleteJsonStoreDataItem(orgName, storeName, itemPath);
            return result != null && result.getResult() == GenericOperationResult.ResultEnum.DELETED;
        });
    }

    public Map<String, Object> findAppInfo(String clusterGroupAlias, String clusterAlias, String namespace) throws ApiException {
        return findAppInfo(orgName(context.defaultVariables()), clusterGroupAlias, clusterAlias, namespace);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> findAppInfo(String org, String clusterGroupAlias, String clusterAlias, String namespace) throws ApiException {
        assertNotEmpty("ClusterGroupAlias", clusterGroupAlias);
        assertNotEmpty("ClusterAlias", clusterAlias);
        assertNotEmpty("Namespace", namespace);

        var appInfo = getAppInfo(org, clusterAlias, namespace);
        if (appInfo != null) {
            return appInfo;
        }

        var result = executeQuery(storeName(context.defaultVariables()), "find-app", Map.of("clusterGroupAlias", clusterGroupAlias, "namespace", namespace));
        if (result == null || result.isEmpty()) {
            return null;
        }
        return (Map<String, Object>) result.get(0);
    }

    public Map<String, Object> getAppInfo(String clusterAlias, String namespace) throws ApiException {
        return getAppInfo(orgName(context.defaultVariables()), clusterAlias, namespace);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAppInfo(String orgName, String clusterAlias, String namespace) throws ApiException {
        assertNotEmpty("Namespace", namespace);

        var itemPath = itemPath(clusterAlias, namespace);

        var storeName = storeName(context.defaultVariables());

        var result = ClientUtils.withRetry(RETRY_COUNT, RETRY_INTERVAL, () ->
                new JsonStoreDataApi(apiClient(defaultVariables(Map.of())))
                        .getJsonStoreData(orgName, storeName, itemPath));

        if (result == null) {
            return null;
        }

        if (result instanceof Map<?, ?>) {
            return (Map<String, Object>) result;
        }

        throw new RuntimeException("Unexpected inventory response type: " + result.getClass());
    }

    private Map<String, Object> defaultInfo(String namespace, String appName) {
        Map<String, Object> data = new HashMap<>();
        data.put("flow", context.variables().getString("flow"));
        data.put("appName", appName);
        data.put("namespace", namespace);
        data.put("account", account());
        data.put("clusterAlias", clusterAlias());
        data.put("clusterGroupAlias", clusterGroupAlias());
        data.put("ck8sRef", ck8sRef());
        data.put("ck8sExtRef", ck8sExtRef());
        data.put("concordProcessId", context.processInstanceId());
        data.put("initiator", initiator(context));
        data.put("lastDeployedAt", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX").format(ZonedDateTime.now()));
        return data;
    }

    private static String initiator(Context context) {
        var initiator = context.processConfiguration().initiator();
        if (initiator == null) {
            return null;
        }
        return MapUtils.getString(initiator, "username");
    }

    private Variables defaultVariables(Map<String, Object> overrides) {
        if (overrides == null) {
            return context.defaultVariables();
        }

        return new MapBackedVariables(MergeUtils.merge(context.defaultVariables().toMap(), overrides));
    }

    private static void assertNotEmpty(String what, String s) {
        if (s == null || s.isEmpty()) {
            throw new IllegalArgumentException(what + " cannot be empty or null");
        }
    }

    private String itemPath(String namespace) {
        return itemPath(clusterAlias(), namespace);
    }

    private static String itemPath(String clusterAlias, String namespace) {
        return String.format("%s/%s", clusterAlias, namespace);
    }

    private static String orgName(Variables defaultVariables) {
        return defaultVariables.assertString("org");
    }

    private static String storeName(Variables defaultVariables) {
        return defaultVariables.getString("appStoreName", "cluster-apps");
    }

    private String clusterGroupAlias() {
        var clusterRequest = clusterRequest();
        return MapUtils.assertString(clusterRequest, "groupAlias");
    }

    private String account() {
        var clusterRequest = clusterRequest();
        return MapUtils.assertString(clusterRequest, "account");
    }

    private String clusterAlias() {
        var clusterRequest = clusterRequest();
        return MapUtils.assertString(clusterRequest, "alias");
    }

    private String ck8sRef() {
        return context.variables().getString("ck8sRef");
    }

    private String ck8sExtRef() {
        return context.variables().getString("ck8sExtRef");
    }

    private Map<String, Object> clusterRequest() {
        return context.variables().assertMap("clusterRequest");
    }

    private ApiClient apiClient(Variables defaultVariables) {
        if (!defaultVariables.has("baseUrl")) {
            return defaultApiClient;
        }

        var baseUrl = defaultVariables.assertString("baseUrl");
        var apiKey = defaultVariables.assertString("apiKey");

        return apiClientFactory.create(ApiClientConfiguration.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build());
    }
}
