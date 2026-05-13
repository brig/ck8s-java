package ca.vanzyl.ck8s;

import com.walmartlabs.concord.runtime.v2.sdk.DryRunReady;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.WorkingDirectory;
import dev.ybrig.ck8s.cli.common.Ck8sPath;
import dev.ybrig.ck8s.cli.common.Ck8sUtils;

import javax.el.LambdaExpression;
import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Named("ck8sClusterCfg")
@DryRunReady
public class Ck8sClusterTask implements Task {

    private final Path workDir;

    @Inject
    public Ck8sClusterTask(WorkingDirectory workingDirectory) {
        this.workDir = workingDirectory.getValue();
    }

    public Map<String, Object> get(String clusterGroupOrAlias) {
        var ck8s = new Ck8sPath(workDir, null);
        return Ck8sUtils.buildClusterRequest(ck8s, clusterGroupOrAlias);
    }

    public List<Map<String, Object>> list(LambdaExpression filter) {
        var ck8s = new Ck8sPath(workDir, null);
        return Ck8sUtils.streamClusterYaml(ck8s)
                .map(p -> Ck8sUtils.buildClusterRequest(ck8s, p))
                .filter(c -> (Boolean)filter.invoke(c))
                .toList();
    }

    @Deprecated
    public Map<String, Object> getByGroup(String ck8sPath, String clusterGroup) {
        var ck8s = new Ck8sPath(Paths.get(ck8sPath), null);
        return Ck8sUtils.buildClusterRequest(ck8s, clusterGroup);
    }
}
