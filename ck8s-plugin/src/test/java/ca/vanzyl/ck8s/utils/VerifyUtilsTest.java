package ca.vanzyl.ck8s.utils;

import ca.vanzyl.ck8s.aws.AwsTaskUtils;
import org.junit.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SchemaAttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.StringAttributeConstraintsType;

import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VerifyUtilsTest {

    @Test
    public void testExactMatch() {
        var existing = Map.of("key", "value");
        var expected = Map.of("key", "value");

        assertTrue(VerifyUtils.verifyPartialMapMatch("exactMatch", existing, expected));
    }

    @Test
    public void testPartialMatch() {
        var existing = Map.of("key", "value", "extra", "ignore me");
        var expected = Map.of("key", "value");

        assertTrue(VerifyUtils.verifyPartialMapMatch("partialMatch", existing, expected));
    }

    @Test
    public void testMissingKeyMismatch() {
        var existing = Map.of("key", "value");
        var expected = Map.of("key", "value", "missing", "oops");

        assertFalse(VerifyUtils.verifyPartialMapMatch("missingKey", existing, expected));
    }

    @Test
    public void testValueMismatch() {
        var existing = Map.of("key", "actual");
        var expected = Map.of("key", "expected");

        assertFalse(VerifyUtils.verifyPartialMapMatch("valueMismatch", existing, expected));
    }

    @Test
    public void testNestedPartialMatch() {
        var existing = Map.of("nested", Map.of("a", "b", "c", "d"));
        var expected = Map.of("nested", Map.of("a", "b"));

        assertTrue(VerifyUtils.verifyPartialMapMatch("nestedPartial", existing, expected));
    }

    @Test
    public void testNestedMismatch() {
        var existing = Map.of("nested", Map.of("a", "wrong", "c", "d"));
        var expected = Map.of("nested", Map.of("a", "right"));

        assertFalse(VerifyUtils.verifyPartialMapMatch("nestedMismatch", existing, expected));
    }

    @Test
    public void testNullMaps() {
        assertFalse(VerifyUtils.verifyPartialMapMatch("nullCheck", null, Map.of()));
        assertFalse(VerifyUtils.verifyPartialMapMatch("nullCheck", Map.of(), null));
    }

    @Test
    public void testNestedDeepMismatch() {
        var existing = Map.of("outer", Map.of("inner", Map.of("k", "wrong")));
        var expected = Map.of("outer", Map.of("inner", Map.of("k", "right")));

        assertFalse(VerifyUtils.verifyPartialMapMatch("deepMismatch", existing, expected));
    }

    @Test
    public void testVerify() {
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

        var b = SchemaAttributeType.builder()
                .name("ad-groups")
                .attributeDataType("String")
                .developerOnlyAttribute(true)
                .mutable(true)
                .required(true)
                .stringAttributeConstraints(StringAttributeConstraintsType.builder()
                        .minLength("2")
                        .maxLength("256")
                        .build())
                .build();

        var result = VerifyUtils.verifyPartialMapMatch("test", AwsTaskUtils.serialize(a), AwsTaskUtils.serialize(b));
        assertFalse(result);
    }

    @Test
    public void testVerifySimple() {
        var a = Map.of("key", "value");

        var b = Map.of("key", "value2");

        var result = VerifyUtils.verifyPartialMapMatch("Test Entity", a, b);
        assertFalse(result);
    }

    @Test
    public void testNestedExactMatch() {
        var existing = Map.of("outer", Map.of("inner", Map.of("k", "v")));
        var expected = Map.of("outer", Map.of("inner", Map.of("k", "v")));

        assertTrue(VerifyUtils.verifyPartialMapMatch("deepMatch", existing, expected));
    }
}
