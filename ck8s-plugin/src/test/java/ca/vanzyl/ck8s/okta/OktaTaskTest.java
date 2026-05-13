package ca.vanzyl.ck8s.okta;

import ca.vanzyl.ck8s.MockTestContext;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;

@Ignore
public class OktaTaskTest {

    @Test
    public void testGetApp() throws Exception {
        var task = new OktaTask(new MockTestContext());
        var input = Map.<String, Object>of(
            "action", "get-app",
            "apiToken", "<>",
            "oauthBaseUrl", "<>",
            "clientId", "<>"
        );
        var result = (TaskResult.SimpleResult)task.execute(new MapBackedVariables(input));
        System.out.println(">>>" + result.ok());
        System.out.println(">>>" + result.values());
    }
}
