package ca.vanzyl.ck8s.mysql;

import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class MysqlExtractHostTest {

    @Test
    public void testStandardUrlWithPortAndDb() {
        String url = "jdbc:mysql://localhost:3306/mydb";
        String result = MysqlTask.extractHostOrDefault(url, null);
        assertEquals("localhost", result);
    }

    @Test
    public void testUrlWithHostnameOnly() {
        String url = "jdbc:mysql://db.example.com/";
        String result = MysqlTask.extractHostOrDefault(url, null);
        assertEquals("db.example.com", result);
    }

    @Test
    public void testUrlWithoutSlash() {
        String url = "jdbc:mysql://host:1234";
        String result = MysqlTask.extractHostOrDefault(url, null);
        assertEquals("host", result);
    }

    @Test
    public void testUrlWithIpAndDb() {
        String url = "jdbc:mysql://192.168.1.10:3306/dbname?param=value";
        String result = MysqlTask.extractHostOrDefault(url, null);
        assertEquals("192.168.1.10", result);
    }

    @Test
    public void testUrlWithTrailingOnlyHost() {
        String url = "jdbc:mysql://hostname";
        String result = MysqlTask.extractHostOrDefault(url, null);
        assertEquals("hostname", result);
    }

    @Test
    public void testNullInputThrows() {
        var result = MysqlTask.extractHostOrDefault(null, "default-host");
        assertEquals("default-host", result);
    }

    @Test
    public void testInvalidPrefixThrows() {
        String url = "jdbc:postgresql://localhost:5432/db";
        Exception ex = assertThrows(UserDefinedException.class, () -> MysqlTask.extractHostOrDefault(url, null));
        assertEquals("Expected JDBC URL to start with 'jdbc:mysql:', got: " + url, ex.getMessage());
    }
}
