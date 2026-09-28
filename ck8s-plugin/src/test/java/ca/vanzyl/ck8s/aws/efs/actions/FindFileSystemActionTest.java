package ca.vanzyl.ck8s.aws.efs.actions;

import ca.vanzyl.ck8s.MockTestContext;
import ca.vanzyl.ck8s.aws.CredentialsProvider;
import ca.vanzyl.ck8s.aws.efs.EfsClientFactory;
import ca.vanzyl.ck8s.aws.efs.EfsTaskParams;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.runtime.common.injector.InstanceId;
import com.walmartlabs.concord.runtime.v2.runner.PersistenceService;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;

import java.util.UUID;

import static org.mockito.Mockito.mock;

@Ignore
public class FindFileSystemActionTest {

    @Test
    public void test() throws Exception {
        var credentialsProvider = new CredentialsProvider(new ObjectMapper(), mock(PersistenceService.class), new InstanceId(UUID.randomUUID()));
        var factory = new EfsClientFactory(credentialsProvider, new InstanceId(UUID.randomUUID()));
        var action = new FindFileSystemAction(factory);

        var params = new EfsTaskParams.FindFileSystemParams(
                new EfsTaskParams.BaseParams("sandbox", Region.US_EAST_1, false),
                "brig-test-efs");

        var result = action.execute(new MockTestContext(), params);
        System.out.println(((TaskResult.SimpleResult)result).values());
    }
}
