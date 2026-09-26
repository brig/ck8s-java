package ca.vanzyl.ck8s.k8s;

import ca.vanzyl.ck8s.common.MapUtils;
import ca.vanzyl.ck8s.common.Mapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.walmartlabs.concord.common.ConfigurationUtils;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.internal.CertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.fabric8.kubernetes.client.Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY;

public final class K8sClientFactory {

    private static final Logger log = LoggerFactory.getLogger(K8sClientFactory.class);

    private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    public static KubernetesClient create(K8sTaskParams input) {
        var builder = new KubernetesClientBuilder();

        var kubeconfigPath = input.baseParams().kubeConfigPath();
        if (kubeconfigPath == null) {
            System.setProperty(KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, "false");
            builder.withConfig(detectClientKeyAlgo(Config.autoConfigure(null)));
        } else {
            try {
                var config = Files.readString(kubeconfigPath);
                builder.withConfig(detectClientKeyAlgo(Config.fromKubeconfig(config)));
                log.info("Using KUBECONFIG: {}", kubeconfigPath);
            } catch (InvalidPathException e) {
                throw new IllegalArgumentException("Invalid env.KUBECONFIG path: %s".formatted(kubeconfigPath));
            } catch (IOException e) {
                throw new RuntimeException("Error reading KUBECONFIG: %s".formatted(kubeconfigPath), e);
            }
        }

        return builder.build();
    }

    // fabric8 leaves clientKeyAlgo at its "RSA" default for the key an exec credential plugin returns,
    // and then reads an ECDSA key as PKCS#1, failing with "Invalid DER: object is not integer". Telling
    // it the key is EC is enough: CertUtils#loadKey has a working path for both EC encodings.
    // Upstream fixes only the armoured case: https://github.com/fabric8io/kubernetes-client/pull/8026
    static Config detectClientKeyAlgo(Config config) {
        if (config.getClientKeyData() == null && config.getClientKeyFile() == null) {
            return config;
        }

        var algo = Config.getKeyAlgorithm(config.getClientKeyFile(), config.getClientKeyData());
        if (algo == null) {
            // pinniped hands back a PKCS#8 key, whose "BEGIN PRIVATE KEY" header names no algorithm
            algo = pkcs8ClientKeyAlgo(config);
        }

        if (algo != null && !algo.equals(config.getClientKeyAlgo())) {
            log.info("kubeconfig: using the detected '{}' client key algorithm", algo);
            config.setClientKeyAlgo(algo);
        }
        return config;
    }

    private static String pkcs8ClientKeyAlgo(Config config) {
        byte[] der;
        try (var pem = CertUtils.getInputStreamFromDataOrFile(config.getClientKeyData(), config.getClientKeyFile())) {
            var armoured = new String(pem.readAllBytes(), StandardCharsets.UTF_8);
            der = Base64.getMimeDecoder().decode(armoured.replaceAll("-----(BEGIN|END)[^-]*-----", ""));
        } catch (IOException | IllegalArgumentException e) {
            log.warn("kubeconfig: cannot read the client key to detect its algorithm: {}", e.getMessage());
            return null;
        }

        for (var candidate : List.of("EC", "RSA")) {
            try {
                KeyFactory.getInstance(candidate).generatePrivate(new PKCS8EncodedKeySpec(der));
                return candidate;
            } catch (GeneralSecurityException e) {
                // not this algorithm, try the next one
            }
        }
        return null;
    }

    public static void patchEnvInKubeconfig(Path path, Map<String, Object> env) throws IOException {
        var cfg = Mapper.yaml().readMap(path);
        var users = MapUtils.getList(cfg, "users");
        for (var user : users) {
            var kubeEnv = new ArrayList<>(MapUtils.getList(user, "user.exec.env"));
            for (var e : env.entrySet()) {
                if (e.getValue() instanceof String s) {
                    replace(kubeEnv, e.getKey(), s);
                }
            }
            ConfigurationUtils.set(user, kubeEnv, "user", "exec", "env");
        }

        objectMapper.writeValue(path.toFile(), cfg);
    }

    private static void replace(List<Map<String, Object>> kubeEnvConfiguration, String key, String value) {
        for (var kubeEnv : kubeEnvConfiguration) {
            if (key.equals(kubeEnv.get("name"))) {
                log.info("kubeconfig patch: replaced env variable '{}' with value '{}'", key, value);
                kubeEnv.put("value", value);
                return;
            }
        }

        var entry = new HashMap<String, Object>();
        entry.put("name", key);
        entry.put("value", value);
        log.info("kubeconfig patch: added env variable '{}' with value '{}'", key, value);
        kubeEnvConfiguration.add(entry);
    }

    private K8sClientFactory() {
    }
}
