package ca.vanzyl.ck8s;

import org.junit.Test;

import static org.junit.Assert.*;

public class Ck8sCryptoTaskTest {

    private final Ck8sCryptoTask task = new Ck8sCryptoTask();

    @Test
    public void testRandomHexLength() {
        String result = task.randomHex(32);
        assertEquals(64, result.length());
    }

    @Test
    public void testRandomHexIsValidHex() {
        String result = task.randomHex(32);
        assertTrue(result.matches("[0-9a-f]+"));
    }

    @Test
    public void testRandomHexDifferentCalls() {
        String a = task.randomHex(32);
        String b = task.randomHex(32);
        assertNotEquals(a, b);
    }

    @Test
    public void testRandomHexSmallSize() {
        String result = task.randomHex(1);
        assertEquals(2, result.length());
        assertTrue(result.matches("[0-9a-f]+"));
    }

    @Test
    public void testGeneratePasswordLength() {
        String result = task.generatePassword(16);
        assertEquals(16, result.length());
    }

    @Test
    public void testGenerateRandomStringNoSpecialChars() {
        String result = task.generateRandomString(20, false);
        assertEquals(20, result.length());
        assertTrue(result.matches("[A-Za-z0-9]+"));
    }
}