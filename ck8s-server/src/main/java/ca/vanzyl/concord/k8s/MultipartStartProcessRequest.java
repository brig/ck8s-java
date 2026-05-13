package ca.vanzyl.concord.k8s;

import com.walmartlabs.concord.server.MultipartUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;

import java.io.InputStream;
import java.util.*;

@Deprecated
public class MultipartStartProcessRequest implements StartProcessRequest {

    public static StartProcessRequest from(MultipartInput input) {
        return new MultipartStartProcessRequest(input);
    }

    private static final String CLUSTER_ALIAS_KEY = "clusterAlias";
    private static final String BUILD_CLUSTER_REQUEST_KEY = "buildClusterRequest";

    private static final String CLIENT_CLUSTER_ALIAS_KEY = "clientClusterAlias";
    private static final String ARGUMENTS_KEY = "arguments";
    private static final String REQUIREMENTS_KEY = "requirements";
    private static final String META_KEY = "meta";
    private static final String FLOW_KEY = "flow";
    private static final String DEBUG_KEY = "debug";
    private static final String CK8S_FLOWS_ARCHIVE_KEY = "ck8sFlowsArchive";
    private static final String CK8S_REF_KEY = "ck8sRef";
    private static final String CK8S_EXT_REF_KEY = "ck8sExtRef";
    private static final String INCLUDE_TESTS_KEY = "includeTests";
    private static final String PARENT_INSTANCE_ID_KEY = "parenInstanceId";
    private static final String EXCLUSIVE_KEY = "exclusive";
    private static final String ACTIVE_PROFILES_KEY = "activeProfiles";
    private static final String OUT_EXPR_KEY = "out";
    private static final String ADDITIONAL_CONCORD_YAML = "additionalConcordYaml";
    private static final String ADDITIONAL_CONCORD_YAML_NAME = "additionalConcordYamlName";
    private static final String ATTACHMENTS = "attachments";
    private static final String DRY_RUN_MODE_KEY = "dryRun";

    private final MultipartInput input;

    public MultipartStartProcessRequest(MultipartInput input) {
        this.input = input;
    }

    @Schema(name = CLUSTER_ALIAS_KEY, type = "string")
    @Override
    public String getClusterAlias() {
        return MultipartUtils.getString(input, CLUSTER_ALIAS_KEY);
    }

    @Schema(name = BUILD_CLUSTER_REQUEST_KEY)
    @Override
    public boolean isBuildClusterRequest() {
        return MultipartUtils.getBoolean(input, BUILD_CLUSTER_REQUEST_KEY, false);
    }

    @Schema(name = CLIENT_CLUSTER_ALIAS_KEY)
    @Override
    public String getClientClusterAlias() {
        return MultipartUtils.assertString(input, CLIENT_CLUSTER_ALIAS_KEY);
    }

    @Override
    public String anyClusterAlias() {
        if (MultipartUtils.contains(input, CLIENT_CLUSTER_ALIAS_KEY)) {
            return getClientClusterAlias();
        }
        return getClusterAlias();
    }

    @Schema(name = "org")
    @Override
    public String getOrg() {
        return MultipartUtils.getString(input, "org");
    }

    @Schema(name = "project")
    @Override
    public String getProject() {
        return MultipartUtils.getString(input, "project");
    }

    @Override
    public boolean hasFlowsArchive() {
        return MultipartUtils.contains(input, CK8S_FLOWS_ARCHIVE_KEY);
    }

    @Schema(name = CK8S_FLOWS_ARCHIVE_KEY, type = "string", format = "path")
    @Override
    public InputStream getFlowsArchive() {
        return MultipartUtils.getStream(input, CK8S_FLOWS_ARCHIVE_KEY);
    }

    @Schema(name = ADDITIONAL_CONCORD_YAML, type = "string", format = "path")
    @Override
    public InputStream getAdditionalConcordYaml() {
        return MultipartUtils.getStream(input, ADDITIONAL_CONCORD_YAML);
    }

    @Override
    public Map<String, InputPart> getAttachments() {
        Map<String, InputPart> attachments = new HashMap<>();

        for (InputPart p : input.getParts()) {
            String name = MultipartUtils.extractName(p);
            if (name != null && name.startsWith("_attachment")) {
                name = name.substring("_attachment".length());
                attachments.put(name, p);
            }
        }

        return attachments;
    }

    @Schema(name = ADDITIONAL_CONCORD_YAML_NAME)
    @Override
    public String getAdditionalConcordYamlName() {
        return MultipartUtils.getString(input, ADDITIONAL_CONCORD_YAML_NAME);
    }

    @Override
    public boolean hasAdditionalConcordYaml() {
        return MultipartUtils.contains(input, ADDITIONAL_CONCORD_YAML);
    }

    @Schema(name = CK8S_REF_KEY)
    @Override
    public String getCk8sRef() {
        return MultipartUtils.getString(input, CK8S_REF_KEY);
    }

    @Schema(name = CK8S_EXT_REF_KEY)
    @Override
    public String getCk8sExtRef() {
        return MultipartUtils.getString(input, CK8S_EXT_REF_KEY);
    }

    @Schema(name = INCLUDE_TESTS_KEY)
    @Override
    public boolean isIncludeTests() {
        return MultipartUtils.getBoolean(input, INCLUDE_TESTS_KEY, false);
    }

    @Schema(name = ARGUMENTS_KEY)
    @Override
    public Map<String, ? extends Object> getArguments() {
        return getMap(input, ARGUMENTS_KEY);
    }

    @Schema(name = DEBUG_KEY)
    @Override
    public boolean isDebug() {
        return MultipartUtils.getBoolean(input, DEBUG_KEY, false);
    }

    @Schema(name = META_KEY)
    @Override
    public Map<String, ? extends Object> getMeta() {
        return getMap(input, META_KEY);
    }

    @Schema(name = FLOW_KEY)
    @Override
    public String getFlowName() {
        return MultipartUtils.assertString(input, FLOW_KEY);
    }

    @Schema(name = REQUIREMENTS_KEY)
    @Override
    public Map<String, Object> getRequirements() {
        return getMap(input, REQUIREMENTS_KEY);
    }

    @Schema(name = EXCLUSIVE_KEY)
    @Override
    public Map<String, Object> getExclusiveOptions() {
        return getMap(input, EXCLUSIVE_KEY);
    }

    @Schema(name = PARENT_INSTANCE_ID_KEY)
    @Override
    public UUID getParentInstanceId() {
        return MultipartUtils.getUuid(input, PARENT_INSTANCE_ID_KEY);
    }

    @Schema(name = ACTIVE_PROFILES_KEY)
    @Override
    public List<String> getActiveProfiles() {
        return MultipartUtils.getStringList(input, ACTIVE_PROFILES_KEY);
    }

    @Schema(name = OUT_EXPR_KEY)
    @Override
    public List<String> getOutExpressions() {
        return MultipartUtils.getStringList(input, OUT_EXPR_KEY);
    }

    @Schema(name = DRY_RUN_MODE_KEY)
    @Override
    public boolean isDryRun() {
        return MultipartUtils.getBoolean(input, DRY_RUN_MODE_KEY, false);
    }

    private static Map<String, Object> getMap(MultipartInput input, String key) {
        Map<String, Object> result = MultipartUtils.getMap(input, key);
        if (result == null) {
            return Collections.emptyMap();
        }
        return result;
    }
}
