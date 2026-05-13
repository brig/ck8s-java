package ca.vanzyl.ck8s.aws;

import ca.vanzyl.ck8s.MockTestContext;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;

@Ignore
public class IamPolicyTaskTest {

    @Test
    public void test_remove() throws Exception {
        IamPolicyTask task = new IamPolicyTask(new MockTestContext(Map.of()), mock(LockService.class), mock(CredentialsProvider.class));

        Map<String, Object> input = Map.of(
                "action", "remove-resources",
                "region", "us-east-1",
                "role", "sandbox-dta-use1",
                "policy", "sandbox-dta-use1-policy",
                "template", "/Users/brig/prj/github/aetion/root/ck8s-ext/ck8s-components/databricks/aws/iam/customers-policy-template.json",
                "templateArgs", Map.of("S3_BUCKET", "brig.ci1.sandbox.com", "S3_FOLDER", "brig-dev")

        );
        task.execute(new MapBackedVariables(input));
    }

    @Test
    public void test_add() throws Exception {
        IamPolicyTask task = new IamPolicyTask(new MockTestContext(Map.of()), mock(LockService.class), mock(CredentialsProvider.class));

        Map<String, Object> input = Map.of(
                "action", "add-resources",
                "region", "us-east-1",
                "role", "sandbox-dta-use1",
                "policy", "sandbox-dta-use1-policy",
                "template", "/Users/brig/prj/github/aetion/root/ck8s-ext/ck8s-components/databricks/aws/iam/customers-policy-template.json",
                "templateArgs", Map.of("S3_BUCKET", "brig.ci1.sandbox.com", "S3_FOLDER", "brig-dev")

        );
        task.execute(new MapBackedVariables(input));
    }
}
