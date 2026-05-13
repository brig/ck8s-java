package ca.vanzyl.ck8s.aws;

import ca.vanzyl.ck8s.MockTestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.junit.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;

public class EcrTaskTest {

    @Test
    public void test() {
        EcrTask task = new EcrTask(new MockTestContext(Map.of()), mock(CredentialsProvider.class), new ObjectMapper());

        Map<String, Object> input = Map.of(
                "action", "tag-image",
                "region", "us-east-1",
                "repositoryName", "projects",
                "version", "fb-integration-tests-9a3d041",
                "tag", "test"
        );

        TaskResult taskResult = task.execute(new MapBackedVariables(input));
        System.out.println(taskResult);
    }
}
