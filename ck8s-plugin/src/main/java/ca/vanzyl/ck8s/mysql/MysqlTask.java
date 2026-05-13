package ca.vanzyl.ck8s.mysql;

import ca.vanzyl.ck8s.common.MapUtils;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.DryRunReady;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Named("ck8sMysql")
@DryRunReady
public class MysqlTask implements Task {

    private final static Logger log = LoggerFactory.getLogger(MysqlTask.class);

    private static final String URL_TEMPLATE = "jdbc:mysql://%s:%d/%s";

    private final boolean dryRunMode;

    @Inject
    public MysqlTask(Context context) {
        this.dryRunMode = context.processConfiguration().dryRun();
    }

    public void waitDbReady(Map<String, Object> mysql) {
        if (dryRunMode) {
            log.info("Running in dry-run mode: Skipping waiting");
            return;
        }

        while (!Thread.currentThread().isInterrupted()) {
            try {
                execQuery(mysql, "select 1", Collections.emptyList());
                log.info("waitDbReady ['{}'] -> ready", mysql);
                return;
            } catch (Exception e) {
                log.info("waitDbReady ['{}'] -> not ready: {}", mysql, e.getMessage());
            }

            sleep(10_000);
        }
    }

    public void createSchema(Map<String, Object> mysql, String databaseName) {
        if (dryRunMode) {
            log.info("Running in dry-run mode: Skipping creating schema '{}'", databaseName);
            return;
        }

        try (Connection conn = connection(mysql)) {
            createSchema(conn, databaseName);
            log.info("createSchema ['{}'] -> ok", databaseName);
        } catch (SQLException e) {
            log.error("createSchema ['{}'] -> error: {}", databaseName, e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public void createSchemas(Map<String, Object> mysql, List<String> databaseNames) {
        if (dryRunMode) {
            log.info("Running in dry-run mode: Skipping creating schemas '{}'", databaseNames);
            return;
        }

        try (Connection conn = connection(mysql)) {
            for (String databaseName : databaseNames) {
                createSchema(conn, databaseName);
            }
        } catch (SQLException e) {
            log.error("createSchemaBatch ['{}'] -> error: {}", databaseNames, e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<List<Object>> execQuery(Map<String, Object> mysql, String sql, List<Object> params) {
        if (dryRunMode) {
            log.info("Running in dry-run mode: Skipping exec query '{}'", sql);
            return List.of();
        }

        try (Connection conn = connection(mysql)) {
            return performQuery(conn, sql, params != null ? params : Collections.emptyList(), rs -> {
                // Get metadata
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    row.add(value);
                }
                return row;
            });
        } catch (SQLException e) {
            log.error("execQuery ['{}', '{}'] -> error: {}", sql, params, e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public void execute(Map<String, Object> mysql, String sql) {
        if (dryRunMode) {
            log.info("Running in dry-run mode: Skipping exec query '{}'", sql);
            return ;
        }

        try (Connection conn = connection(mysql)) {
            int result = execute(conn, sql);
            log.info("execQuery ['{}'] -> ok (rows: {})", sql, result);
        } catch (SQLException e) {
            log.error("execute ['{}'] -> error: {}", sql, e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    private void createSchema(Connection conn, String databaseName) throws SQLException {
        String sql = String.format("CREATE SCHEMA IF NOT EXISTS `%s`", escape(databaseName));
        execute(conn, sql);
    }

    public void dropSchema(Map<String, Object> mysql, String databaseName) {
        if (dryRunMode) {
            log.info("Running in dry-run mode: Skipping dropping schema '{}'", databaseName);
            return;
        }

        try (Connection conn = connection(mysql)) {
            dropSchema(conn, databaseName);
            log.info("dropSchema ['{}'] -> done", databaseName);
        } catch (SQLException e) {
            log.error("dropSchema ['{}'] -> error", databaseName, e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public void dropSchemas(Map<String, Object> mysql, List<String> databaseNames) {
        if (dryRunMode) {
            log.info("Running in dry-run mode: Skipping dropping schemas '{}'", databaseNames);
            return;
        }

        try (Connection conn = connection(mysql)) {
            for (String databaseName : databaseNames) {
                dropSchema(conn, databaseName);
            }
        } catch (SQLException e) {
            log.error("dropSchemaBatch ['{}'] -> error", databaseNames, e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private void dropSchema(Connection conn, String databaseName) throws SQLException {
        String sql = String.format("DROP SCHEMA IF EXISTS `%s`", escape(databaseName));
        execute(conn, sql);
    }

    public void createUserAndGrantPermissions(Map<String, Object> mysql, String name, String password, List<String> schemas) {
        if (dryRunMode) {
            log.info("Running in dry-run mode: Skipping creating user '{}'", name);
            return;
        }

        try (Connection conn = connection(mysql)) {
            boolean created = createUser(conn, name, password);
            if (created) {
                log.info("✅ createUser ['{}'] -> ok", name);
            } else {
                log.info("createUser ['{}'] -> user already exists", name);
            }

            for (String schema : schemas) {
                grantAllPrivileges(conn, schema, name);
                log.info("grantAllPrivileges ['{}', '{}'] -> ok", schema, name);
            }

            flushPrivileges(conn);
        } catch (Exception e) {
            log.error("createUser ['{}'] -> error", name, e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public void rotateUserPassword(Map<String, Object> mysql, String name, String password) {
        if (dryRunMode) {
            log.info("Running in dry-run mode: Skipping rotate password '{}'", name);
            return;
        }

        var sql = "alter user `%s`@`%%` identified by '%s' retain current password";

        try (Connection conn = connection(mysql)) {
            execute(conn, String.format(sql, escape(name), escape(password)));
        } catch (Exception e) {
            log.error("rotateUserPassword ['{}'] -> error", name, e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public void createUser(Map<String, Object> mysql, String name, String password) {
        if (dryRunMode) {
            log.info("Running in dry-run mode: Skipping creating user '{}'", name);
            return;
        }

        try (Connection conn = connection(mysql)) {
            boolean created = createUser(conn, name, password);
            if (created) {
                log.info("createUser ['{}'] -> ok", name);
            } else {
                log.info("createUser ['{}'] -> user already exists", name);
            }
        } catch (Exception e) {
            log.error("createUser ['{}'] -> error", name, e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private boolean createUser(Connection conn, String name, String password) throws SQLException {
        String userExistsSql = "select count(*) from mysql.user where User = ?";
        String createUserSql = "create user if not exists `%s`@`%%` identified by '%s'";

        boolean userExists = count(conn, userExistsSql, List.of(name)) > 0;
        if (userExists) {
            return false;
        }

        execute(conn, String.format(createUserSql, escape(name), escape(password)));
        return true;
    }

    public void dropUser(Map<String, Object> mysql, String name) {
        String rootUser = MapUtils.assertString(mysql, "rootUsername");
        if (rootUser.equals(name)) {
            return;
        }

        if (dryRunMode) {
            log.info("Running in dry-run mode: Skipping dropping user '{}'", name);
            return;
        }

        try (Connection conn = connection(mysql)) {
            dropUser(conn, name);
            log.info("dropUser ['{}'] -> ok", name);
        } catch (Exception e) {
            log.error("dropUser ['{}'] -> error", name, e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private void dropUser(Connection conn, String name) throws SQLException {
        String sql = "DROP USER IF EXISTS `%s`";
        execute(conn, String.format(sql, escape(name)));
    }

    public void grantAllPrivileges(Map<String, Object> mysql, String schema, String user) {
        if (dryRunMode) {
            log.info("Running in dry-run mode: Skipping grant all privileges on '{}' to '{}'", schema, user);
            return;
        }

        try (Connection conn = connection(mysql)) {
            grantAllPrivileges(conn, schema, user);
            log.info("grantAllPrivileges ['{}', '{}'] -> ok", schema, user);
        } catch (Exception e) {
            log.error("grantAllPrivileges ['{}', '{}'] -> error", schema, user, e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private void grantAllPrivileges(Connection conn, String schema, String user) throws SQLException {
        String grantSql = "GRANT ALL PRIVILEGES ON `%s`.* TO `%s`@`%%`";
        execute(conn, String.format(grantSql, escape(schema), escape(user)));
    }

    private void flushPrivileges(Connection conn) throws SQLException {
        execute(conn, "FLUSH PRIVILEGES");
    }

    public void dropAllPrivileges(Map<String, Object> mysql, String schema, String user) {
        if (dryRunMode) {
            log.info("Running in dry-run mode: Skipping dropping user '{}' privileges on '{}'", user, schema);
            return;
        }

        String cntSql = "select count(*) from mysql.db where User = ? AND Db = ?";
        try {
            tx(mysql, conn -> {
                int count = count(conn, cntSql, List.of(user, schema));

                if (count > 0) {
                    dropAllPrivileges(conn, schema, user);
                    log.info("dropAllPrivileges ['{}', '{}'] -> ok", schema, user);
                }
            });
        } catch (Exception e) {
            log.error("dropAllPrivileges ['{}', '{}'] -> error", schema, user, e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private void dropAllPrivileges(Connection conn, String schema, String user) throws SQLException {
        String grantSql = "REVOKE ALL PRIVILEGES ON `%s`.* FROM `%s`@`%%`";
        execute(conn, String.format(grantSql, escape(schema), escape(user)));
    }

    public void createInstance(Map<String, Object> mysql, String name, String user, String password) {
        if (dryRunMode) {
            log.info("Running in dry-run mode: Skipping create instance '{}' with user '{}'", name, user);
            return;
        }

        try {
            log.info("createInstance ['{}', '{}', '{}']", mysql, name, user);
            tx(mysql, conn -> {
                createSchema(conn, name);
                createUser(conn, user, password);
                grantAllPrivileges(conn, name, user);
            });
        } catch (Exception e) {
            log.error("createInstance ['{}', '{}'] -> error", name, user, e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public void deleteInstance(Map<String, Object> mysql, String name, String user) {
        if (dryRunMode) {
            log.info("Running in dry-run mode: Skipping deleting instance '{}' with user '{}'", name, user);
            return;
        }

        String usersInSchemaSql = "select count(*) from mysql.db where Db = ? and User <> ?";
        String schemaForUserSql = "select count(*) from mysql.db where User = ?";

        try {
            tx(mysql, conn -> {
                dropAllPrivileges(conn, name, user);

                int additionalUsersInSchema = count(conn, usersInSchemaSql, List.of(name, user));
                if (additionalUsersInSchema == 0) {
                    // no more users in schema
                    dropSchema(mysql, name);
                }

                int userSchemas = count(conn, schemaForUserSql, List.of(user));
                if (userSchemas == 0) {
                    String rootUser = MapUtils.assertString(mysql, "rootUsername");
                    if (!rootUser.equals(user)) {
                        // no more schema for user
                        dropUser(conn, user);
                        log.info("dropUser ['{}'] -> done", user);
                    }
                }
            });
        } catch (Exception e) {
            log.error("deleteInstance ['{}', '{}'] -> error", name, user, e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public static String sanitizeUsername(String input) {
        if (input == null || input.isEmpty()) {
            throw new UserDefinedException("Username cannot be null or empty");
        }

        // Keep only allowed characters: letters, digits, _, @, $, .
        String sanitized = input.replaceAll("[^a-zA-Z0-9$.]", "_");

        // Remove invalid characters from the start and end
        sanitized = sanitized.replaceAll("^[^a-zA-Z0-9]+", "");
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9]+$", "");

        // Trim to max 32 characters (MySQL limit)
        if (sanitized.length() > 32) {
            sanitized = sanitized.substring(0, 32);
        }

        // If nothing is left after cleaning — generate a deterministic fallback
        if (sanitized.isEmpty()) {
            sanitized = "user_" + hashOf(input);
        }

        return sanitized.toLowerCase();
    }

    public static String extractSchemaOrDefault(String jdbcUrl, String defaultSchema) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return defaultSchema;
        }

        if (!jdbcUrl.startsWith("jdbc:mysql:")) {
            throw new UserDefinedException("Expected JDBC URL to start with 'jdbc:mysql:', got: " + jdbcUrl);
        }

        var slashIndex = jdbcUrl.indexOf("/", "jdbc:mysql://".length());
        if (slashIndex == -1 || slashIndex == jdbcUrl.length() - 1) {
            throw new UserDefinedException("JDBC URL does not contain a schema: " + jdbcUrl);
        }

        var queryIndex = jdbcUrl.indexOf("?", slashIndex);
        var schema = (queryIndex != -1)
                ? jdbcUrl.substring(slashIndex + 1, queryIndex)
                : jdbcUrl.substring(slashIndex + 1);

        if (schema.isEmpty()) {
            throw new UserDefinedException("Schema name is empty in JDBC URL: " + jdbcUrl);
        }
        return schema;
    }

    public static String extractHostOrDefault(String jdbcUrl, String defaultHost) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return defaultHost;
        }

        if (!jdbcUrl.startsWith("jdbc:mysql://")) {
            throw new UserDefinedException("Expected JDBC URL to start with 'jdbc:mysql:', got: " + jdbcUrl);
        }

        var urlWithoutPrefix = jdbcUrl.substring("jdbc:mysql://".length());
        int slashIndex = urlWithoutPrefix.indexOf('/');
        var hostPort = (slashIndex != -1) ? urlWithoutPrefix.substring(0, slashIndex) : urlWithoutPrefix;

        int colonIndex = hostPort.indexOf(':');
        return (colonIndex != -1) ? hostPort.substring(0, colonIndex) : hostPort;
    }

    private static String hashOf(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            // Take first 4 bytes (8 hex characters) — short but reasonably unique :)
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static void tx(Map<String, Object> mysql, Tx tx) throws SQLException {
        try (Connection conn = connection(mysql)) {
            conn.setAutoCommit(false);

            try {
                tx.execute(conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
            }

            conn.setAutoCommit(true);
        }
    }

    private static int execute(Connection conn, String sql) throws SQLException {
        try (Statement statement = conn.createStatement()) {
            return statement.executeUpdate(sql);
        }
    }

    private static int count(Connection conn, String sql, List<Object> params) throws SQLException {
        Integer result = singleResult(conn, sql, params, rs -> rs.getInt(1));
        if (result == null) {
            return 0;
        }
        return result;
    }

    private static <T> T singleResult(Connection conn, String sql, List<Object> params, ResultSetConverter<T> converter) throws SQLException {
        List<T> results = performQuery(conn, sql, params, converter);

        if (results.isEmpty()) {
            return null;
        } else if (results.size() > 1) {
            throw new IllegalStateException("Expected single result, got: " + results.size() + ", results: " + results);
        }

        return results.get(0);
    }

    private static <T> List<T> performQuery(Connection conn, String sql, List<Object> params, ResultSetConverter<T> converter) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

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

    private static Connection connection(Map<String, Object> mysql) throws SQLException {
        String host = MapUtils.assertString(mysql, "host");
        int port = MapUtils.getNumber(mysql, "port", 3306).intValue();
        String schema = MapUtils.getString(mysql, "schema", "mysql");
        String rootUser = MapUtils.assertString(mysql, "rootUsername");
        String rootPassword = MapUtils.assertString(mysql, "rootPassword");
        String url = (String.format(URL_TEMPLATE, host, port, schema));

        try {
            return DriverManager.getConnection(url, rootUser, rootPassword);
        } catch (Exception e) {
            log.error("connection ['{}''] -> error: {}", url, e.getMessage());
            throw e;
        }
    }

    // dumb and simple
    private static String escape(String name) {
        return name.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"");
    }

    interface ResultSetConverter<T> {

        T convert(ResultSet rs) throws SQLException;
    }

    interface Tx {

        void execute(Connection conn) throws SQLException;
    }

    private static void sleep(long ms)
    {
        try {
            Thread.sleep(ms);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
