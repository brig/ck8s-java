package ca.vanzyl.ck8s.k8s;

import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

@Ignore("requires a running Kubernetes pod")
public class KubernetesClientTaskTest {

    @Test
    public void verifyExec() throws Exception {
        var task = new KubernetesClientTask();
        var input = new MapBackedVariables(Map.of(
                "action", "exec",
                "namespace", "default",
                "podName", "test",
                "cmd", List.of("sh", "-c", "ls -l")
        ));
        var result = task.execute(input);
        assertTrue(result instanceof TaskResult.SimpleResult);
        assertTrue(((TaskResult.SimpleResult) result).ok());
    }
}
