package ca.vanzyl.ck8s.utils;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StringUtilsTaskTest
{

    private final StringUtilsTask task = new StringUtilsTask();

    @Test
    public void testTruncateString()
    {
        // Test case 1: String length is less than maxLength
        String originalString1 = "Short";
        int maxLength1 = 10;
        String truncatedString1 = task.truncate(originalString1, maxLength1);
        assertTrue(maxLength1 >= truncatedString1.length());
        Assert.assertEquals(originalString1, truncatedString1);

        // Test case 2: String length is greater than maxLength, even number of characters
        String originalString2 = "This is a long string that needs to be truncated.";
        int maxLength2 = 20;
        String expectedTruncatedString2 = "This is ...runcated.";
        String truncatedString2 = task.truncate(originalString2, maxLength2);
        assertEquals(maxLength2, truncatedString2.length());
        Assert.assertEquals(expectedTruncatedString2, truncatedString2);

        // Test case 3: String length is greater than maxLength, odd number of characters
        String originalString3 = "This is a longer string that needs to be truncated.";
        int maxLength3 = 23;
        String expectedTruncatedString3 = "This is a ...truncated.";
        String truncatedString3 = task.truncate(originalString3, maxLength3);
        assertEquals(maxLength3, truncatedString3.length());
        Assert.assertEquals(expectedTruncatedString3, truncatedString3);

        // Test case 4: maxLength is 3
        String originalString4 = "This is a long string that needs to be truncated.";
        int maxLength4 = 3;
        try {
            task.truncate(originalString4, maxLength4);
            Assert.fail("Expected IllegalArgumentException was not thrown.");
        }
        catch (IllegalArgumentException e) {
            Assert.assertEquals("maxLength should be greater than 3", e.getMessage());
        }

        // Test case 5:
        String originalString5 = "This is!";
        int maxLength5 = 4;
        String expectedTruncatedString5 = "...!";
        String truncatedString5 = task.truncate(originalString5, maxLength5);
        assertEquals(maxLength5, truncatedString5.length());
        Assert.assertEquals(expectedTruncatedString5, truncatedString5);
    }

    @Test
    public void testTruncateClean()
    {
        String originalString = "This is a long string that needs to be truncated.";
        int maxLength = 10;
        String truncatecString = task.truncateClean(originalString, maxLength);
        assertEquals(maxLength, truncatecString.length());
    }

    @Test
    public void testTrimTrailingSpaces_WithTrailingSpaces() {
        String original = " Hello world   ";
        String expected = " Hello world";
        assertEquals(expected, StringUtilsTask.stripEnd(original));
    }

    @Test
    public void testTrimTrailingSpaces_WithTrailingLineBreaks() {
        String original = " Hello world \n\r";
        String expected = " Hello world";
        assertEquals(expected, StringUtilsTask.stripEnd(original));
    }

    @Test
    public void testTrimTrailingSpaces_WithNoTrailingSpaces() {
        String original = "Hello world";
        String expected = "Hello world";
        assertEquals(expected, StringUtilsTask.stripEnd(original));
    }

    @Test
    public void testTrimTrailingSpaces_EmptyString() {
        String original = "";
        String expected = "";
        assertEquals(expected, StringUtilsTask.stripEnd(original));
    }

    @Test
    public void testTrimTrailingSpaces_NullInput() {
        String original = null;
        String expected = null;
        assertEquals(expected, StringUtilsTask.stripEnd(original));
    }
}
