package ca.vanzyl.ck8s.aws;

import org.junit.Ignore;

@Ignore
public class IamTaskTest {
//
//    private CredentialsProvider credentialsProvider;
//
//    @Before
//    public void setup() {
//        credentialsProvider = mock(CredentialsProvider.class);
//        when(credentialsProvider.get(any(Variables.class))).thenReturn(DefaultCredentialsProvider.builder()
//                .profileName("sandbox")
//                .build());
//    }
//
//    @Test
//    public void testCreateRole() throws Exception {
//        var roleName = "my-test-role";
//        var trustPolicy =
//                """
//                        {
//                          "Version": "2012-10-17",
//                          "Statement": [
//                            {
//                              "Effect": "Allow",
//                              "Principal": {
//                                "AWS": "*"
//                              },
//                              "Action": "sts:AssumeRole"
//                            }
//                          ]
//                        }
//                        """;
//
//        var input = Map.<String, Object>of(
//                "action", "create-role",
//                "region", "us-east-1",
//                "role", roleName,
//                "trustPolicy", trustPolicy
//        );
//
//        var task = new IamTask(credentialsProvider, new MockTestContext(Map.of()));
//        task.execute(new MapBackedVariables(input));
//    }
//
//    @Test
//    public void testAttachPolicy() throws Exception {
//        var roleName = "my-test-role";
//
//        var input = Map.<String, Object>of(
//                "action", "attach-role-policy",
//                "region", "us-east-1",
//                "role", roleName,
//                "policy", "arn:aws:iam::aws:policy/AdministratorAccess"
//        );
//
//        var task = new IamTask(credentialsProvider, new MockTestContext(Map.of()));
//        task.execute(new MapBackedVariables(input));
//    }
//
//    @Test
//    public void testDeleteRole() throws Exception {
//        var roleName = "my-test-role";
//        var input = Map.<String, Object>of(
//                "action", "delete-role",
//                "region", "us-east-1",
//                "role", roleName
//        );
//
//        var task = new IamTask(credentialsProvider, new MockTestContext(Map.of()));
//        task.execute(new MapBackedVariables(input));
//    }
//
//    @Test
//    public void testListPolicies() throws Exception {
//        var roleName = "ci1-g-sa-concord3";
//        var input = Map.<String, Object>of(
//                "action", "list-role-policies",
//                "region", "us-east-1",
//                "role", roleName
//        );
//
//        var task = new IamTask(credentialsProvider, new MockTestContext(Map.of()));
//        var result = (TaskResult.SimpleResult)task.execute(new MapBackedVariables(input));
//        System.out.println(result.values());
//
//        input = Map.<String, Object>of(
//                "action", "delete-role-policies",
//                "region", "us-east-1",
//                "role", roleName,
//                "policyNames", result.values().get("inlinePolicies"),
//                "policyArns", result.values().get("attachedPolicyArns")
//        );
//
//        task.execute(new MapBackedVariables(input));
//
//        String policyDocument = """
//            {
//              "Version": "2012-10-17",
//              "Statement": [
//                {
//                  "Effect": "Allow",
//                  "Action": "ec2:DescribeInstances",
//                  "Resource": "*"
//                },
//                {
//                  "Effect": "Allow",
//                  "Action": "s3:ListBucket",
//                  "Resource": "arn:aws:s3:::example-bucket"
//                }
//              ]
//            }
//            """;
//
//        input = Map.<String, Object>of(
//                "action", "replace-role-policy",
//                "region", "us-east-1",
//                "role", roleName,
//                "policyName", "default",
//                "policyDocument", policyDocument
//        );
//
//        task.execute(new MapBackedVariables(input));
//    }
//
//    @Test
//    public void testReplaceManagedPolicy() throws Exception {
//        var policyDocument = """
//            {
//              "Version": "2012-10-17",
//              "Statement": [
//                {
//                  "Effect": "Allow",
//                  "Action": "ec2:DescribeInstances",
//                  "Resource": "*"
//                },
//                {
//                  "Effect": "Allow",
//                  "Action": "s3:ListBucket",
//                  "Resource": "arn:aws:s3:::example-bucket"
//                }
//              ]
//            }
//            """;
//
//        var input = Map.<String, Object>of(
//                "action", "replace-managed-policy",
//                "region", "us-east-1",
//                "policy", "arn:aws:iam::123456789:policy/my-test-policy",
//                "policyName", "my-test-policy",
//                "policyDocument", policyDocument
//        );
//
//        var task = new IamTask(credentialsProvider, new MockTestContext(Map.of()));
//        var result = (TaskResult.SimpleResult)task.execute(new MapBackedVariables(input));
//        System.out.println(result.values());
//    }
}
