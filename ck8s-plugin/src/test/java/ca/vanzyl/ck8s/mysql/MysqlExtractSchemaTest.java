package ca.vanzyl.ck8s.mysql;

import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import org.junit.Test;

import static org.junit.Assert.*;

public class MysqlExtractSchemaTest {

    @Test
    public void testValidJdbcUrl() {
        String url = "jdbc:mysql://host:3306/shared_svc_ap_prod_coding";
        String schema = MysqlTask.extractSchemaOrDefault(url, null);
        assertEquals("shared_svc_ap_prod_coding", schema);
    }

    @Test
    public void testJdbcUrlWithQueryParams() {
        var url = "jdbc:mysql://host:3306/my_database?useSSL=false&serverTimezone=UTC";
        var schema = MysqlTask.extractSchemaOrDefault(url, null);
        assertEquals("my_database", schema);
    }

    @Test
    public void testDefaultSchema() {
        var schema = MysqlTask.extractSchemaOrDefault(null, "default-schema");
        assertEquals("default-schema", schema);
    }

    @Test
    public void testJdbcUrlWithoutSchema() {
        var url = "jdbc:mysql://host:3306/";
        var e = assertThrows(UserDefinedException.class, () -> {
            MysqlTask.extractSchemaOrDefault(url, null);
        });
        assertTrue(e.getMessage().contains("does not contain a schema"));
    }

    @Test
    public void testJdbcUrlWithoutTrailingSlash() {
        var url = "jdbc:mysql://host:3306";
        var e = assertThrows(UserDefinedException.class, () -> {
            MysqlTask.extractSchemaOrDefault(url, null);
        });
        assertEquals("JDBC URL does not contain a schema: jdbc:mysql://host:3306", e.getMessage());
    }

    @Test
    public void testInvalidJdbcUrl() {
        var url = "not-a-valid-url";
        var e = assertThrows(UserDefinedException.class, () -> {
            MysqlTask.extractSchemaOrDefault(url, null);
        });
        assertTrue(e.getMessage().contains("Expected JDBC URL to start with"));
    }
}
