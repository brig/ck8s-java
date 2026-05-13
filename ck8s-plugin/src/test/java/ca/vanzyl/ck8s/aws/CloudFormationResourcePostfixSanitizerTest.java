package ca.vanzyl.ck8s.aws;

import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import org.junit.Test;

import static ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTask.resourcePart;
import static ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTask.sanitizeStackName;
import static org.junit.Assert.*;

public class CloudFormationResourcePostfixSanitizerTest {

    @Test
    public void testSimpleName() {
        assertEquals("CustomerName", resourcePart("customer name"));
    }

    @Test
    public void testNameWithHyphens() {
        assertEquals("CustomerName", resourcePart("customer-name"));
    }

    @Test
    public void testNameWithUnderscores() {
        assertEquals("CustomerName", resourcePart("customer_name"));
    }

    @Test
    public void testNameWithMixedSeparators() {
        assertEquals("CustomerName123", resourcePart("customer_name-123"));
    }

    @Test
    public void testNameWithLeadingAndTrailingSpaces() {
        assertEquals("CustomerName", resourcePart("   customer name   "));
    }

    @Test
    public void testEmptyString() {
        assertThrows(UserDefinedException.class, () -> resourcePart(""));
    }

    @Test
    public void testOnlySeparators() {
        assertThrows(UserDefinedException.class, () -> resourcePart("___---   "));
    }

    @Test
    public void testAlreadyPascalCase() {
        assertEquals("CustomerName", resourcePart("CustomerName"));
    }

    @Test
    public void testSingleWord() {
        assertEquals("Customer", resourcePart("customer"));
    }

    @Test
    public void testNumbersPreserved() {
        assertEquals("Customer123", resourcePart("customer 123"));
    }
}
