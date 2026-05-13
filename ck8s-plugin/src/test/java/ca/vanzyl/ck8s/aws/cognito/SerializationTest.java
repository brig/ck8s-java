package ca.vanzyl.ck8s.aws.cognito;

import ca.vanzyl.ck8s.aws.AwsTaskUtils;
import ca.vanzyl.ck8s.common.Mapper;
import org.junit.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SchemaAttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.StringAttributeConstraintsType;

public class SerializationTest {

    @Test
    public void testSerialization() {
        var a = SchemaAttributeType.builder()
                .name("ad-groups")
                .attributeDataType("String")
                .developerOnlyAttribute(true)
                .mutable(true)
                .required(true)
                .stringAttributeConstraints(StringAttributeConstraintsType.builder()
                        .minLength("1")
                        .maxLength("256")
                        .build())
                .build();

        var m = AwsTaskUtils.serialize(a);
        System.out.println(Mapper.yaml().writeAsString(m));
    }
}
