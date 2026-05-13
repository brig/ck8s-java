package ca.vanzyl.ck8s.mysql;

import ca.vanzyl.ck8s.MockTestContext;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;

import java.sql.*;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MysqlTaskTest {

    @ClassRule
    public static MySQLContainer<?> mysql = new MySQLContainer<>();

    private static final String ROOT_USERNAME = "root";
    private static final String ROOT_PASSWORD = "test";

    private MysqlTask task;

    @Before
    public void setup() {
        task = new MysqlTask(new MockTestContext(Map.of()));
    }

    @Test
    public void testCreateSchema() throws Exception {
        String schemaName = "testSchema";

        task.createSchema(mysqlParams(mysql), schemaName);

        assertSchemaExists(schemaName);

        task.dropSchema(mysqlParams(mysql), schemaName);

        assertSchemaNotExists(schemaName);
    }

    @Test
    public void testExecQuery() throws Exception {
        String schemaName = "testSchema";

        List<List<Object>> result = task.execQuery(mysqlParams(mysql), "SELECT 1 AS number", null);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).size());
        assertEquals(1L, result.get(0).get(0));
    }

    @Test
    public void testCreateUser() throws Exception {
        String schema = "mysql";
        String user = "testUser";
        String password = "test\\Password";

        task.createUser(mysqlParams(mysql), user, password);
        task.grantAllPrivileges(mysqlParams(mysql), schema, user);

        assertCanConnect(schema, user, password);

        task.dropAllPrivileges(mysqlParams(mysql), schema, user);

        assertNoConnect(schema, user, password);

        task.dropUser(mysqlParams(mysql), user);

        assertNoConnect(schema, user, password);

        assertCanConnect(schema, ROOT_USERNAME, ROOT_PASSWORD);
    }

    @Test
    public void testCreateUser2() throws Exception {
        String user = "testUser2";
        String password = "test\\Password";

        task.createUser(mysqlParams(mysql), user, password);
        task.createUser(mysqlParams(mysql), user, password);
    }

    @Test
    public void testCreateInstance() throws Exception {
        String schema = "schema42";
        String user = "testUser42";
        String password = "test\\Password42";

        task.createInstance(mysqlParams(mysql), schema, user, password);
        assertCanConnect(schema, user, password);

        task.deleteInstance(mysqlParams(mysql), schema, user);
        assertNoConnect(schema, user, password);

        assertSchemaNotExists(schema);
        assertCanConnect("mysql", ROOT_USERNAME, ROOT_PASSWORD);
    }

    @Test
    public void testCreateInstanceWithRootUser() throws Exception {
        String schema = "schema48";
        String user = ROOT_USERNAME;
        String password = ROOT_PASSWORD;

        task.createInstance(mysqlParams(mysql), schema, user, password);
        assertCanConnect(schema, user, password);

        task.deleteInstance(mysqlParams(mysql), schema, user);
        assertSchemaNotExists(schema);

        assertCanConnect("mysql", ROOT_USERNAME, ROOT_PASSWORD);
    }

    private void assertSchemaExists(String name) throws Exception {
        String checkSchemaExists = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?";
        String result = singleResult(mysql, checkSchemaExists, List.of(name), resultSet -> resultSet.getString(1));
        if (result == null) {
            throw new IllegalStateException("Schema '" + name + "' not found");
        }
    }

    private void assertSchemaNotExists(String name) throws Exception {
        String checkSchemaExists = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?";
        String result = singleResult(mysql, checkSchemaExists, List.of(name), resultSet -> resultSet.getString(1));
        if (result != null) {
            throw new IllegalStateException("Schema '" + name + "' exists");
        }
    }

    private void assertCanConnect(String schema, String user, String password) throws Exception {
        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s", mysql.getHost(), mysql.getFirstMappedPort(), schema);
        List<String> result = performQuery(jdbcUrl, user, password, "select 1", Collections.emptyList(), rs -> rs.getString(1));
        assertEquals(1, result.size());
    }

    private void assertNoConnect(String schema, String user, String password) throws Exception {
        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s", mysql.getHost(), mysql.getFirstMappedPort(), schema);
        try {
            performQuery(jdbcUrl, user, password, "select 1", Collections.emptyList(), rs -> rs.getString(1));
        } catch (SQLException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Access denied for user '" + user + "'"));
        }
    }

    private static Map<String, Object> mysqlParams(MySQLContainer<?> mysql) {
        Map<String, Object> params = new HashMap<>();
        params.put("host", mysql.getHost());
        params.put("port", mysql.getFirstMappedPort());
        params.put("schema", mysql.getDatabaseName());
        params.put("rootUsername", ROOT_USERNAME);
        params.put("rootPassword", ROOT_PASSWORD);
        return params;
    }

    private <T> T singleResult(JdbcDatabaseContainer<?> container, String sql, List<Object> params, ResultSetConverter<T> converter) throws Exception {
        List<T> results = performQuery(container, sql, params, converter);

        if (results.isEmpty()) {
            return null;
        } else if (results.size() > 1) {
            throw new IllegalStateException("Expected single result, got: " + results.size() + ", results: " + results);
        }

        return results.get(0);
    }

    private <T> List<T> performQuery(JdbcDatabaseContainer<?> container, String sql, List<Object> params, ResultSetConverter<T> converter) throws Exception {
        return performQuery(container.getJdbcUrl(), ROOT_USERNAME, ROOT_PASSWORD, sql, params, converter);
    }

    private <T> List<T> performQuery(String jdbcUrl, String user, String password, String sql, List<Object> params, ResultSetConverter<T> converter) throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                ps.setObject(i + 1, p);
            }

            List<T> result = new ArrayList<>();
            try (ResultSet resultSet = ps.executeQuery()) {
                while (resultSet.next()) {
                    result.add(converter.convert(resultSet));
                }
            }
            return result;
        }
    }

    interface ResultSetConverter<T> {

        T convert(ResultSet rs) throws Exception;
    }
}
