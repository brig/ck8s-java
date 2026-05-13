package ca.vanzyl.ck8s.utils;

import ca.vanzyl.ck8s.common.MapUtils;
import ca.vanzyl.ck8s.common.Mapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.client2.ApiClient;
import com.walmartlabs.concord.client2.ApiException;
import com.walmartlabs.concord.client2.ProcessEntry;
import com.walmartlabs.concord.client2.ProcessV2Api;
import com.walmartlabs.concord.runtime.v2.model.Profile;
import com.walmartlabs.concord.runtime.v2.runner.context.ContextVariablesWithOverrides;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import com.walmartlabs.concord.sdk.LogTags;
import dev.ybrig.ck8s.cli.common.Ck8sPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.el.LambdaExpression;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static dev.ybrig.ck8s.cli.common.Ck8sUtils.buildClusterRequest;
import static dev.ybrig.ck8s.cli.common.Ck8sUtils.findClustersYaml;

@Named("ck8sUtils")
@DryRunReady
public class Ck8sUtils
        implements Task
{

    private static final Logger log = LoggerFactory.getLogger(Ck8sUtils.class);

    private final Context context;
    private final ApiClient apiClient;

    @Inject
    public Ck8sUtils(Context context, ApiClient apiClient) {
        this.context = context;
        this.apiClient = apiClient;
    }

    public static String extractPath(String url, int pathIndex) {
        if (url == null || url.isBlank() || pathIndex < 0) {
            return url;
        }

        var parts = url.split("/");

        if (pathIndex < parts.length) {
            return parts[pathIndex];
        } else {
            return url;
        }
    }

    // TODO: add to the project configuration what clusterAlias they can handle
    public String projectName(String clusterAliasOrGroup) {
        String groupName = clusterAliasOrGroup;
        if (groupName.endsWith("-b") || groupName.endsWith("-g")) {
            groupName = groupName.substring(0, groupName.length() - "-b".length());
        }
        return String.format("%-3s", groupName).replace(' ', '_');
    }

    public Map<String, Object> processDefinitionArgs() {
        return new LinkedHashMap<>(context.execution().processDefinition().configuration().arguments());
    }

    public Map<String, Object> profilesArgs(List<String> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return Map.of();
        }

        var result = new LinkedHashMap<String, Object>();
        for (var profileName : profiles ) {
            var profile = context.execution().processDefinition().profiles().getOrDefault(profileName, Profile.builder().build());
            result.putAll(profile.configuration().arguments());
        }
        return result;
    }

    public Map<String, Object> forkArgsFromProfile(String flowName, String maybeProfileName) {
        var profile = context.execution().processDefinition().profiles().getOrDefault(maybeProfileName, Profile.builder().build());

        Map<String, Object> result = new LinkedHashMap<>(context.execution().processDefinition().configuration().arguments());
        result.putAll(profile.configuration().arguments());
        result.put("flow", flowName);
        result.put("inputArgs", profile.configuration().arguments());
        return result;
    }

    public static boolean isMap(Object v) {
        return v instanceof Map<?,?>;
    }

    // TODO: remove after concord 2.24.x released
    public static Map<String, Object> newMap() {
        return new LinkedHashMap<>();
    }

    public static Map<String, Object> copyMap(Map<String, Object> m) {
        if (m == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(m);
    }

    public static Map<String, Object> listToMap(List<Map<String, Object>> list, String keyAttr, String valueAttr) {
        if (list == null || list.isEmpty()) {
            return newMap();
        }

        return list.stream()
                .collect(Collectors.toMap(item -> String.valueOf(item.get(keyAttr)), item -> item.get(valueAttr)));
    }

    public static Map<String, Object> listToMap(List<Map<String, Object>> list, String keyAttr) {
        if (list == null || list.isEmpty()) {
            return newMap();
        }

        return list.stream()
                .collect(Collectors.toMap(item -> String.valueOf(item.get(keyAttr)), item -> item));
    }

    public List<Map<String, Object>> loadClusterRequestsForGroup(String clusterGroup) throws IOException {
        List<Map<String, Object>> clusterRequests;
        if (Files.exists(context.workingDirectory().resolve("clusters"))) {
            clusterRequests = K8sFileUtils.listFiles(context.workingDirectory().resolve("clusters").toString()).stream()
                    .map(p -> Mapper.yaml().readMap(p))
                    .filter(r -> clusterGroup.equals(MapUtils.getString(r, "clusterGroup.alias")))
                    .toList();
        } else{
            var ck8sPath = new Ck8sPath(context.workingDirectory(), null);
            clusterRequests = findClustersYaml(ck8sPath, clusterGroup).stream()
                .map(c -> buildClusterRequest(ck8sPath, c))
                .toList();
        }

        log.info("requests loaded: {}", clusterRequests.stream().map(r -> r.get("clusterName")).toList());

        var runtime = context.execution().runtime();
        var ee = runtime.getService(ExpressionEvaluator.class);

        List<Map<String, Object>> result = new ArrayList<>();
        for (var clusterRequestPlain : clusterRequests) {
            MapUtils.set(clusterRequestPlain, "fake", "secretsProvider");

            Map<String, Object> args = Map.of("clusterRequest", clusterRequestPlain);

            var evalContext = EvalContext.builder()
                    .context(context)
                    .variables(new ContextVariablesWithOverrides(context, args))
                    .useIntermediateResults(true)
                    .build();

            result.add(ee.evalAsMap(evalContext, clusterRequestPlain));
        }
        return result;
    }

    public String envSpecificFile(String fileName) {
        Map<String, Object> clusterRequest = context.variables().getMap("clusterRequest", null);
        if (clusterRequest == null) {
            throw new RuntimeException("The clusterRequest is null. Cannot proceed.");
        }

        Path p = Paths.get(fileName);
        String dir = p.getParent() != null ? p.getParent().toString() : "";
        String name = p.getFileName().toString();
        String extension = "";

        int dotIndex = name.lastIndexOf('.');
        if (dotIndex != -1) {
            name = name.substring(0, dotIndex);
            extension = p.getFileName().toString().substring(dotIndex);
        }

        String env = MapUtils.assertString(clusterRequest, "environment");
        String envSpecificName = dir +"/" + name + "-" + env + extension;
        if (Files.exists(Paths.get(envSpecificName))) {
            return envSpecificName;
        }
        return fileName;
    }

    public static String[] splitByDot(String input) {
        if (input == null || input.isEmpty()) {
            return new String[0];
        }
        return input.split("\\.");
    }

    public static Object set(Map<String, Object> m, String path, Object value) {
        MapUtils.set(m, value, path);
        return m;
    }

    public static Map<String, Object> listToMap(List<Map<String, Object>> listOfMaps) {
        return listOfMaps.stream()
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> replacement));
    }

    public static Map<String, Object> listToMap(List<Map<String, Object>> listOfMaps, LambdaExpression mergeFunction) {
        return listOfMaps.stream()
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> mergeFunction.invoke(new Object[]{existing, replacement})));
    }

    public static String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return Arrays.stream(input.split("-"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining());
    }

    public static Object removeEmptyKeyValue(Object input) throws Exception {
        if (input == null) {
            return null;
        }

        var om = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        var str = om.writeValueAsString(input);
        return om.readValue(str, Object.class);
    }

    /**
     * converts map to serializable map
     * workaround for https://github.com/walmartlabs/concord/pull/714
     */
    public static Map<String, Object> toMap(Map<String, Object> m)
    {
        return new LinkedHashMap<>(m);
    }

    /**
     * remove after this PR released https://github.com/walmartlabs/concord/pull/712
     */
    public static void exception(String message)
    {
        throw new UserDefinedException(message);
    }

    // TODO: to concord task
    public void assertFinished(List<String> ids) throws ApiException {
        var result = getProcessStatuses(ids);
        boolean hasFailed = false;
        for (var p : result) {
            if (p.getStatus() == ProcessEntry.StatusEnum.FAILED) {
                log.info("Failed process: {}", LogTags.instanceId(p.getInstanceId()));
                hasFailed = true;
            }
        }

        if (hasFailed) {
            throw new UserDefinedException("Found failed processes");
        }
    }

    private List<ProcessEntry> getProcessStatuses(List<String> ids) throws ApiException {
        ProcessV2Api api = new ProcessV2Api(apiClient);

        List<ProcessEntry> result = new ArrayList<>();
        for (String id : ids) {
            ProcessEntry e = ClientUtils.withRetry(3, 1000,
                    () -> api.getProcess(UUID.fromString(id), Collections.emptySet()));
            result.add(e);
        }
        return result;
    }
}
