package ca.vanzyl.ck8s.aws;

import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import org.junit.Test;

import static ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTask.sanitizeStackName;
import static org.junit.Assert.*;

public class CloudFormationStackNameSanitizerTest {

    @Test
    public void testValidNameRemains() {
        String name = "MyValidStackName";
        assertEquals("MyValidStackName", sanitizeStackName(name));
    }

    @Test
    public void testInvalidCharsReplaced() {
        String name = "My*Stack@Name!";
        assertEquals("My-Stack-Name-", sanitizeStackName(name));
    }

    @Test
    public void testStartsWithNonLetter() {
        String name = "123Stack";
        assertEquals("A123Stack", sanitizeStackName(name));
    }

    @Test
    public void testTooLongTruncated() {
        String name = "Stack".repeat(50);
        String result = sanitizeStackName(name);
        assertTrue(result.length() <= 128);
    }

    @Test
    public void testEmptyThrowsException() {
        assertThrows(UserDefinedException.class, () -> sanitizeStackName(""));
    }
}
