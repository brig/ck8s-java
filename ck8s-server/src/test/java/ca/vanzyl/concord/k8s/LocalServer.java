package ca.vanzyl.concord.k8s;

import ca.vanzyl.concord.k8s.db.DatabaseModule;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Module;
import com.typesafe.config.Config;
import com.walmartlabs.concord.it.testingserver.TestingConcordServer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class LocalServer {

    private static final String TEST_ADMIN_TOKEN = "ck8s";

    public static void main(String[] args) throws Exception {
        var serverPort = 8001;
        try (var db = new PostgreSQLContainer<>("postgres:15-alpine");
             var server = new TestingConcordServer(db, serverPort, createConfig(), extraModules())) {

            db.start();
            server.start();

            System.out.printf("""
                    ==============================================================

                      UI (hosted): http://localhost:%s

                      DB:
                        JDBC URL: %s
                        username: %s
                        password: %s
                      API:
                        admin key: %s

                      curl -i -H 'Authorization: %s' http://localhost:%s/api/v1/org

                    ==============================================================
                    %n""", serverPort, db.getJdbcUrl(), db.getUsername(), db.getPassword(), TEST_ADMIN_TOKEN,
                    TEST_ADMIN_TOKEN, serverPort);

            var uiFiles = System.getenv("BASE_RESOURCE_PATH");
            if (uiFiles == null || uiFiles.isBlank()) {
                System.out.println("""
                        WARNING: BASE_RESOURCE_PATH is not set.

                        If you wish to use Console (aka "the UI"), point BASE_RESOURCE_PATH environment variable to
                        the console2/build directory in your local copy of walmartlabs/concord.

                        The console2 module must be built beforehand.

                        Ignore this warning if you do not care about the UI.

                        ==============================================================
                        """);
            }

            Thread.currentThread().join();
        }
    }

    private static Map<String, String> createConfig() {
        return ImmutableMap.<String, String>builder()
                .put("db.changeLogParameters.defaultAdminToken", TEST_ADMIN_TOKEN)
                .build();
    }

    private static List<Function<Config, Module>> extraModules() {
        return List.of(
                _cfg -> new DatabaseModule(),
                _cfg -> new Ck8sModule());
    }
}
