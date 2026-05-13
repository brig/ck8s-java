package ca.vanzyl.ck8s.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class K8sFileUtilsTest {

    @Test
    public void testAddBeforeFilename_RegularPath() {
        String result = K8sFileUtils.addPrefixToFileName("configs/account/dev/account-level-config.yaml", "mydir/");
        assertEquals("configs/account/dev/mydir/account-level-config.yaml", result);
    }

    @Test
    public void testAddBeforeFilename_PathWithNoDirectory() {
        String result = K8sFileUtils.addPrefixToFileName("account-level-config.yaml", "mydir/");
        assertEquals("mydir/account-level-config.yaml", result);
    }

    @Test
    public void testAddBeforeFilename_EmptyStringToAdd() {
        String result = K8sFileUtils.addPrefixToFileName("configs/account-level-config.yaml", "");
        assertEquals("configs/account-level-config.yaml", result);
    }

    @Test
    public void testAddBeforeFilename_EmptyFilePath() {
        String result = K8sFileUtils.addPrefixToFileName("", "mydir/");
        assertEquals("mydir/", result);
    }

    @Test
    public void testAddBeforeFilename_NullFilePath() {
        String result = K8sFileUtils.addPrefixToFileName(null, "mydir/");
        assertNull("Result should be null for null file path", result);
    }
}
