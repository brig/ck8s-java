package ca.vanzyl.ck8s.cloudformation;

import org.junit.Test;

import java.util.Set;

public class CloudFormationTest {

    @Test
    public void test() {
        CloudFormation cloudFormation = new CloudFormation();
        cloudFormation.statement(new Statement(
                Statement.ALLOW,
                Set.of("cognito-idp:DescribeUserPool"),
                Set.of(Resource.sub("#{CognitoUserPoolId}"))));

        cloudFormation.statement(new Statement(
                Statement.ALLOW,
                Set.of("cognito-idp:DescribeUserPool"),
                Set.of(Resource.plain("*"))));

        System.out.println(Serializer.serialize(cloudFormation));
    }
}
