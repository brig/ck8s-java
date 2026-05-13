package ca.vanzyl.concord.k8s;

import ca.vanzyl.ck8s.common.MapUtils;
import com.walmartlabs.concord.common.ConfigurationUtils;
import com.walmartlabs.concord.sdk.Constants;
import com.walmartlabs.concord.server.process.Payload;
import com.walmartlabs.concord.server.process.ProcessException;
import dev.ybrig.ck8s.cli.common.Ck8sConstants;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

public final class PayloadUtils {

    public static boolean isCk8sProcess(Payload payload) {
        var processType = ck8sProcessVersion(payload);
        if (processType == null) {
            return false;
        }
        return processType.startsWith(Ck8sConstants.PROCESS_TYPE);
    }

    public static String ck8sProcessVersion(Payload payload) {
        var project = payload.getHeader(Payload.PROJECT_DEFINITION);
        if (project == null) {
            return null;
        }

        return MapUtils.getString(project.configuration().asMap(), "meta." + Ck8sConstants.Meta.PROCESS_TYPE_KEY);
    }

    public static Map<String, Object> args(Payload payload) {
        var cfg = payload.getHeader(Payload.CONFIGURATION, Map.of());
        return MapUtils.getMap(cfg, Constants.Request.ARGUMENTS_KEY, Map.of());
    }

    public static Map<String, Object> requirements(Payload payload) {
        var cfg = payload.getHeader(Payload.CONFIGURATION, Map.of());
        return MapUtils.getMap(cfg, Constants.Request.REQUIREMENTS, Map.of());
    }

    public static Payload mergeArg(Payload payload, String key, Object value) {
        return mergeCfg(payload, Constants.Request.ARGUMENTS_KEY, Map.of(key, value));
    }

    public static Payload mergeCfg(Payload payload, String key, Object value) {
        var cfg = payload.getHeader(Payload.CONFIGURATION);
        if (cfg == null) {
            cfg = new HashMap<>();
        }

        return payload.putHeader(Payload.CONFIGURATION,
                ConfigurationUtils.deepMerge(cfg, Map.of(key, value)));
    }

    public static String clientCluster(Payload payload) {
        try {
            return MapUtils.assertString(args(payload), Ck8sConstants.Arguments.CLIENT_CLUSTER);
        } catch (IllegalArgumentException e) {
            throw new ProcessException(payload.getProcessKey(), "Invalid request data format", e, Response.Status.BAD_REQUEST);
        }
    }

    public static String flow(Payload payload) {
        return MapUtils.assertString(args(payload), Ck8sConstants.Arguments.FLOW);
    }

    private PayloadUtils() {
    }
}
