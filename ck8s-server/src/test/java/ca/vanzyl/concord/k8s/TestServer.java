package ca.vanzyl.concord.k8s;

import ca.vanzyl.concord.k8s.db.DatabaseModule;
import com.typesafe.config.Config;
import com.walmartlabs.concord.config.ConfigModule;
import com.walmartlabs.concord.server.ConcordServer;
import com.walmartlabs.concord.server.ConcordServerModule;
import com.google.inject.Module;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class TestServer {

    public static void main(String[] args) throws Exception {
        var config = ConfigModule.load("concord-server");
        var system = new ConcordServerModule(config);
        var allModules = Stream.concat(extraModules().stream().map(f -> f.apply(config)), Stream.of(system)).toList();

        ConcordServer.withModules(allModules)
                .start();

        Thread.currentThread().join();
    }

    private static List<Function<Config, Module>> extraModules() {
        return List.of(
                _cfg -> new DatabaseModule(),
                _cfg -> new Ck8sModule());
    }
}
