package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.MockTestContext;
import ca.vanzyl.ck8s.aws.CredentialsProvider;
import ca.vanzyl.ck8s.aws.StsTask;
import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskParams;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.runtime.common.injector.InstanceId;
import com.walmartlabs.concord.runtime.v2.runner.PersistenceService;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;

@Ignore
public class VerifyInlinePolicyActionTest {

    private static final String POLICY = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Action": "s3:ListAllMyBuckets",
                  "Resource": "*"
                }
              ]
            }
            """;

    @Test
    public void test() throws Exception {
        var context = new MockTestContext();
        var credentialsProvider = new CredentialsProvider(new ObjectMapper(), mock(PersistenceService.class), new InstanceId(UUID.randomUUID()));
        var factory = new IamClientFactory(credentialsProvider, new InstanceId(UUID.randomUUID()));
        var action = new VerifyInlinePolicyAction(factory);

        new StsTask(credentialsProvider, context).execute(new MapBackedVariables(Map.of(
                "action", "assume-role",
                "roleArn", "arn:aws:iam::123456789:role/brig-test-ro-role",
                "roleSessionName", "test",
                "region", "us-east-1",
                "profile", "sandbox"))
        );

        var params = new IamTaskParams.PutRolePolicyParams(
                new IamTaskParams.BaseParams("sandbox", Region.US_EAST_1, false),
                "brig-test-role", "test-inline-policy", POLICY);

        var result = action.execute(new MockTestContext(), params);
        System.out.println(result);
    }

}
