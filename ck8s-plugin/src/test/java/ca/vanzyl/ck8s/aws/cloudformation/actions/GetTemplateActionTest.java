package ca.vanzyl.ck8s.aws.cloudformation.actions;

import ca.vanzyl.ck8s.MockTestContext;
import ca.vanzyl.ck8s.aws.CredentialsProvider;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationClientFactory;
import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTaskParams;
import ca.vanzyl.ck8s.aws.efs.EfsClientFactory;
import ca.vanzyl.ck8s.aws.efs.EfsTaskParams;
import ca.vanzyl.ck8s.aws.efs.actions.FindAccessPointAction;
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
public class GetTemplateActionTest {

    @Test
    public void test() throws Exception {
        var credentialsProvider = new CredentialsProvider(new ObjectMapper(), mock(PersistenceService.class), new InstanceId(UUID.randomUUID()));
        var factory = new CloudFormationClientFactory(credentialsProvider, new InstanceId(UUID.randomUUID()));
        var action = new GetTemplateAction(factory);

        var params = new CloudFormationTaskParams.GetTemplateParams(
                new CloudFormationTaskParams.BaseParams("sandbox", Region.US_EAST_1),
                "my-stack-name"
        );

        var result = action.execute(new MockTestContext(), params);
        System.out.println(((TaskResult.SimpleResult)result).values());
    }

}
