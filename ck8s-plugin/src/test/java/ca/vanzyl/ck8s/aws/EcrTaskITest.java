package ca.vanzyl.ck8s.aws;

import ca.vanzyl.ck8s.MockTestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore
public class EcrTaskITest {

    private CredentialsProvider credentialsProvider;

    @Before
    public void setup() {
        credentialsProvider = mock(CredentialsProvider.class);
        when(credentialsProvider.get(any(Variables.class))).thenReturn(DefaultCredentialsProvider.builder()
                .profileName("dev")
                .build());
    }

    @Test
    public void testDescribeImages() {
        var input = Map.<String, Object>of(
                "action", "describe-images",
                "region", "us-east-1",
                "repositoryName", "fe-study-builder",
                "imageTagPattern", "fb-PE-3630.*"
        );

        var task = new EcrTask(new MockTestContext(Map.of()), credentialsProvider, createObjectMapper());
        var result = (TaskResult.SimpleResult)task.execute(new MapBackedVariables(input));
        System.out.println(result.values());
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        return om;
    }
}
