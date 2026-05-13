package ca.vanzyl.ck8s.k8s.actions;

import ca.vanzyl.ck8s.MockTestContext;
import ca.vanzyl.ck8s.k8s.K8sTaskParams;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Objects;

@Ignore
public class ListEventsActionTest {

    @Test
    public void test() throws Exception{
        var kubeconfig = Path.of(Objects.requireNonNull(System.getenv("KUBECONFIG")));

        var action = new ListEventsAction();
        var params = new K8sTaskParams.ListEventsParams(new K8sTaskParams.BaseParams(kubeconfig), "brig", true);

        var result = action.execute(new MockTestContext(), params);
    }
}
