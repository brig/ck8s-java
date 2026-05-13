package ca.vanzyl.ck8s.utils;

import org.junit.Test;

import java.util.Map;

import static ca.vanzyl.ck8s.utils.MapUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

public class MapUtilsTest
{

    @Test
    public void validateMapUtilsMapExtraction()
    {
        Map<String, Object> clusterRequest = Map.of(
                "ingress",
                Map.of("annotations",
                        Map.of("external",
                                Map.of(
                                        "kubernetes.io/ingress.class", "nginx",
                                        "cert-manager.io/cluster-issuer", "${clusterRequest.certManager.clusterIssuer}"))
                ));

        assertThat(clusterRequestMap(clusterRequest, "ingress.annotations.external"))
                .containsEntry("kubernetes.io/ingress.class", "nginx")
                .containsEntry("cert-manager.io/cluster-issuer", "${clusterRequest.certManager.clusterIssuer}");
    }

    @Test
    public void validateMapUtilsScalarExtraction()
    {
        Map<String, Object> clusterRequest = Map.of(
                "polaris",
                Map.of("auth",
                        Map.of(
                                "enabled", true,
                                "type", "basic")));

        assertThat(clusterRequestBoolean(clusterRequest, "polaris.auth.enabled")).isEqualTo(true);
        assertThat(clusterRequestString(clusterRequest, "polaris.auth.type")).isEqualTo("basic");
    }
}
