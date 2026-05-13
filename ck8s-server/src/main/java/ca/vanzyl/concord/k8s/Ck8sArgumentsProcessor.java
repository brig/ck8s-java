package ca.vanzyl.concord.k8s;

import ca.vanzyl.ck8s.common.MergeUtils;
import com.walmartlabs.concord.sdk.Constants;
import com.walmartlabs.concord.server.process.Payload;
import com.walmartlabs.concord.server.sdk.process.CustomEnqueueProcessor;
import dev.ybrig.ck8s.cli.common.Ck8sConstants;
import dev.ybrig.ck8s.cli.common.Ck8sPath;
import dev.ybrig.ck8s.cli.common.Ck8sUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static ca.vanzyl.concord.k8s.PayloadUtils.*;

public class Ck8sArgumentsProcessor implements CustomEnqueueProcessor {

    private static final Logger log = LoggerFactory.getLogger(Ck8sArgumentsProcessor.class);

    @Override
    public Payload handleConfiguration(Payload payload) {
        if (!isCk8sProcess(payload)) {
            return payload;
        }

        return applyMeta(
                prepareClusterRequestArg(
                        applyRequirements(payload)));
    }

    private static Payload applyRequirements(Payload payload) {
        var clientCluster = clientCluster(payload);
        var currentRequirements = requirements(payload);
        var requirements = MergeUtils.merge(currentRequirements, Map.of("agent", Map.of("clusterAlias", clientCluster)));
        log.info("applyRequirements: {}, {}, {}", clientCluster, currentRequirements, requirements);
        return mergeCfg(payload, Constants.Request.REQUIREMENTS, requirements);
    }

    private Payload prepareClusterRequestArg(Payload payload) {
        var processVersion = ck8sProcessVersion(payload);
        if (!Ck8sConstants.PROCESS_TYPE.equals(processVersion)) {
            return payload;
        }
        
        var workDir = payload.getHeader(Payload.WORKSPACE_DIR);
        var clientCluster = clientCluster(payload);
        log.info("prepareClusterRequestArg: {}", clientCluster);

        var clusterRequest = Ck8sUtils.buildClusterRequest(new Ck8sPath(workDir, null), clientCluster);
        return mergeArg(payload, Ck8sConstants.Arguments.CLUSTER_REQUEST, clusterRequest);
    }

    // just to see the flow name in the metadata/process-ui before starting the process execution
    private Payload applyMeta(Payload payload) {
        return mergeCfg(payload, Constants.Request.META, Map.of(Ck8sConstants.Arguments.FLOW, flow(payload)));
    }
}
