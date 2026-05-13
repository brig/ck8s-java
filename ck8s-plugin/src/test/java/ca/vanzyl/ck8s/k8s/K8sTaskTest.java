package ca.vanzyl.ck8s.k8s;

import ca.vanzyl.ck8s.MockPreviewRecorder;
import ca.vanzyl.ck8s.MockTestContext;
import ca.vanzyl.ck8s.k8s.actions.*;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

import static ca.vanzyl.ck8s.k8s.K8sTaskParams.*;
import static org.junit.Assert.assertTrue;

@Ignore
public class K8sTaskTest {

    @Test
    public void verifyCreateNamespace() throws Exception {
        var kubeconfig = Path.of(Objects.requireNonNull(System.getenv("KUBECONFIG")));

        var action = new CreateNamespaceAction();
        var result = action.execute(new MockTestContext(), new CreateNamespaceParams(new BaseParams(kubeconfig), "brig", Map.of("key", "value")));
        assertOk(result);
    }

    @Test
    public void verifyDeleteNamespace() throws Exception {
        var kubeconfig = Path.of(Objects.requireNonNull(System.getenv("KUBECONFIG")));

        var action = new DeleteNamespaceAction();
        var result = action.execute(new MockTestContext(), new DeleteNamespaceParams(new BaseParams(kubeconfig), "brig"));
        assertOk(result);
    }

    @Test
    public void verifyPreviewCreateNamespace() throws Exception {
        var kubeconfig = Path.of(Objects.requireNonNull(System.getenv("KUBECONFIG")));

        var action = new CreateNamespacePreviewAction(new MockPreviewRecorder());
        var result = action.execute(new MockTestContext(Map.of()), new CreateNamespaceParams(new BaseParams(kubeconfig), "brig", Map.of("key", "value")));
        assertOk(result);
    }

    @Test
    public void verifyPreviewDeleteNamespace() throws Exception {
        var kubeconfig = Path.of(Objects.requireNonNull(System.getenv("KUBECONFIG")));

        var action = new DeleteNamespacePreviewAction(new MockPreviewRecorder());
        var result = action.execute(new MockTestContext(), new DeleteNamespaceParams(new BaseParams(kubeconfig), "brig"));
        assertOk(result);
    }

    @Test
    public void getSecretTest() throws Exception {
        var kubeconfig = Path.of(Objects.requireNonNull(System.getenv("KUBECONFIG")));

        var action = new GetSecretDataAction();
        var result = action.execute(new MockTestContext(), new GetSecretParams(new BaseParams(kubeconfig), "dds", "dds-postgres-credentials2"));
        System.out.println(((TaskResult.SimpleResult) result).values());
        assertOk(result);
    }

    @Test
    public void createSecretTest() throws Exception {
        var kubeconfig = Path.of(Objects.requireNonNull(System.getenv("KUBECONFIG")));

        var data = Map.of("username",  Base64.getEncoder().encodeToString("u".getBytes()), "password",  Base64.getEncoder().encodeToString("****".getBytes()));

        var action = new UpsertSecretAction();
        var result = action.execute(new MockTestContext(), new CreateSecretParams(new BaseParams(kubeconfig), "test", "test-secret", data));
        System.out.println(((TaskResult.SimpleResult) result).values());
        assertOk(result);
    }

    private static void assertOk(TaskResult result) {
        assertTrue(result instanceof TaskResult.SimpleResult);
        assertTrue(((TaskResult.SimpleResult) result).ok());
    }
}
