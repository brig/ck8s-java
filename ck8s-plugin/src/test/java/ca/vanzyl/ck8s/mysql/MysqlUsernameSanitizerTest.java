package ca.vanzyl.ck8s.mysql;

import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import org.junit.Test;

import static org.junit.Assert.*;

public class MysqlUsernameSanitizerTest {

    @Test
    public void testSanitize_ValidUsername() {
        String input = "valid_user123";
        String result = MysqlTask.sanitizeUsername(input);
        assertEquals("valid_user123", result);
    }

    @Test
    public void testSanitize_RemoveInvalidChars() {
        String input = "user@#%^&*()_name!";
        String result = MysqlTask.sanitizeUsername(input);
        assertEquals("user_________name", result);
    }

    @Test
    public void testSanitize_TrimToMaxLength() {
        String input = "averylongusernamethatexceeds32characterslong";
        String result = MysqlTask.sanitizeUsername(input);
        assertEquals(32, result.length());
    }

    @Test
    public void testSanitize_RemoveInvalidStartEnd() {
        String input = "!!!username!!!";
        String result = MysqlTask.sanitizeUsername(input);
        assertEquals("username", result);
    }

    @Test
    public void testSanitize_EmptyAfterCleaning() {
        String input = "!@#$%^&*()";
        String result = MysqlTask.sanitizeUsername(input);
        assertTrue(result.startsWith("user_"));
        assertEquals(13, result.length()); // "user_" + 8 hex chars
    }

    @Test
    public void testSanitize_NullInputThrowsException() {
        assertThrows(UserDefinedException.class, () -> MysqlTask.sanitizeUsername(null));
    }

    @Test
    public void testSanitize_EmptyInputThrowsException() {
        assertThrows(UserDefinedException.class, () -> MysqlTask.sanitizeUsername(""));
    }

    @Test
    public void testSanitize_DeterministicFallback() {
        String input = "!@#$";
        String first = MysqlTask.sanitizeUsername(input);
        String second = MysqlTask.sanitizeUsername(input);
        assertEquals(first, second); // should always produce the same fallback
    }
}
