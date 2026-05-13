package ca.vanzyl.ck8s.context;

import ca.vanzyl.concord.k8s.ImmutablesYamlMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.walmartlabs.concord.client2.ApiClient;
import com.walmartlabs.concord.client2.ApiException;
import com.walmartlabs.concord.client2.JsonStoreDataApi;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.sdk.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.*;

import static ca.vanzyl.ck8s.utils.YamlUtils.nindentYaml;

@Named("k8sContext")
@SuppressWarnings("unused")
public class CK8sContextTask
        implements Task
{

    private final static Logger logger = LoggerFactory.getLogger(CK8sContextTask.class);

    private final Context context;
    private final ApiClient apiClient;

    private final String orgName;

    @Inject
    public CK8sContextTask(Context context, ApiClient apiClient)
    {
        this.context = context;
        this.apiClient = apiClient;
        this.orgName = context.processConfiguration().projectInfo().orgName();
    }

    public K8sCluster cluster()
            throws Exception
    {
        ClusterInventoryClient client = client();

        //
        // Always make sure the inventory for clusters has been created
        //
        client.createInventory(orgName);

        String clusterId = clusterAlias();
        K8sCluster cluster = client.getCluster(orgName, clusterId);
        if (cluster == null) {
            cluster = ImmutableK8sCluster.builder()
                    .id(clusterId)
                    .build();
            client.updateCluster(orgName, cluster);
        }
        return cluster;
    }

    // --------------------------------------------------------------------------------------------------------------------
    // Helm
    // --------------------------------------------------------------------------------------------------------------------

    public void chart(String name, String version, String namespace)
            throws Exception
    {
        // No one needs this, and this store just keeps growing constantly...

//        Chart chart = ImmutableChart.builder()
//                .name(name)
//                .version(version)
//                .namespace(namespace)
//                .build();
//        logger.info("Adding chart: {}", chart);
//        K8sCluster cluster = cluster();
//        List<Chart> charts = new ArrayList<>(cluster.charts());
//        charts.add(chart);
//        K8sCluster updatedCluster = ImmutableK8sCluster
//                .copyOf(cluster)
//                .withCharts(charts);
//        ClusterInventoryClient client = client();
//        client.updateCluster(orgName, updatedCluster);
    }

    public void chartImages(String name, String version, List<String> images) throws ApiException {
        String clusterId = clusterAlias();
        Map<String, Object> chartImages = new HashMap<>();
        chartImages.put("chart", name);
        chartImages.put("version", version);
        chartImages.put("images", images);

        JsonStoreDataApi client = new JsonStoreDataApi(apiClient);
        client.updateJsonStoreData(orgName, "chartImages", clusterId + "/" + name + "/" + version, chartImages);
    }

    // --------------------------------------------------------------------------------------------------------------------
    // Features
    // --------------------------------------------------------------------------------------------------------------------

    public void enableFeature(String feature)
            throws Exception
    {
        logger.info("Enabling feature: {}", feature);
        K8sCluster cluster = cluster();
        Set<String> updatedEnabledFeatures = Sets.newHashSet(cluster.enabledFeatures());
        updatedEnabledFeatures.add(feature);
        K8sCluster updatedCluster = ImmutableK8sCluster
                .copyOf(cluster)
                .withEnabledFeatures(updatedEnabledFeatures);
        ClusterInventoryClient client = client();
        client.updateCluster(orgName, updatedCluster);
    }

    public String featureEnabled(String feature)
            throws Exception
    {
        K8sCluster cluster = cluster();
        Set<String> enabledFeatures = cluster.enabledFeatures();
        if (enabledFeatures.contains(feature)) {
            return "true";
        }
        else {
            return "false";
        }
    }

    // --------------------------------------------------------------------------------------------------------------------
    // Ingress annotations
    // --------------------------------------------------------------------------------------------------------------------

    public void ingressAnnotation(String ingressAnnotation)
            throws Exception
    {
        logger.info("Adding ingress annotation: {} for {}/{}", ingressAnnotation, orgName, clusterAlias());
        K8sCluster cluster = cluster();
        Set<String> updatedIngressAnnotations = Sets.newHashSet(cluster.ingressAnnotations());
        updatedIngressAnnotations.add(ingressAnnotation);
        K8sCluster updatedCluster = ImmutableK8sCluster
                .copyOf(cluster)
                .withIngressAnnotations(updatedIngressAnnotations);
        ClusterInventoryClient client = client();
        client.updateCluster(orgName, updatedCluster);
    }

    public String ingressAnnotations(int indentCount)
            throws Exception
    {
        List<String> ingressAnnotations = Lists.newArrayList(ingressAnnotations());
        String indent = String.join("", Collections.nCopies(indentCount, " "));
        StringBuilder sb = new StringBuilder(System.lineSeparator());
        int size = ingressAnnotations.size();
        for (int i = 0; i < size; i++) {
            String ingressAnnotation = ingressAnnotations.get(i);
            sb.append(indent).append(ingressAnnotation);
            if (i != (size - 1)) {
                sb.append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    public Set<String> ingressAnnotations()
            throws Exception
    {
        K8sCluster cluster = cluster();
        return cluster.ingressAnnotations();
    }

    public String ingressAnnotationsFor(String component, int indent)
    {
        return nindentYaml(ingressAnnotationsFor(component), indent);
    }

    public Map<String, String> ingressAnnotationsFor(String component)
    {
        Map<String, Object> clusterRequest = clusterRequest();

        // External annotations
        Map<String, String> external = ca.vanzyl.ck8s.common.MapUtils.getMap(clusterRequest, "ingress.annotations.external", Collections.emptyMap());
        Map<String, String> annotations = new HashMap<>(external);

        Map<String, String> componentConfiguration = ca.vanzyl.ck8s.common.MapUtils.getMap(clusterRequest, component, null);
        if (componentConfiguration != null) {
            // Auth annotations
            String ingressClass = ca.vanzyl.ck8s.common.MapUtils.getString(clusterRequest, "ingress.class");
            boolean authEnabled = ca.vanzyl.ck8s.common.MapUtils.getBoolean(clusterRequest, component + ".auth.enabled", false);
            if (ingressClass != null && authEnabled) {
                String authType = ca.vanzyl.ck8s.common.MapUtils.getString(clusterRequest, component + ".auth.type");
                Map<String, String> authAnnotations = ca.vanzyl.ck8s.common.MapUtils.getMap(clusterRequest, "ingress." + ingressClass + ".annotations.auth." + authType, Collections.emptyMap());
                annotations.putAll(authAnnotations);
            }
        }
        return annotations;
    }

    // --------------------------------------------------------------------------------------------------------------------
    // Post manifests
    // --------------------------------------------------------------------------------------------------------------------

    public void postManifest(String manifest)
            throws Exception
    {
        logger.info("Adding post manifest: {} for {}/{}", manifest, orgName, clusterAlias());
        K8sCluster cluster = cluster();
        Set<String> updatedPostManifests = Sets.newHashSet(cluster.postManifests());
        updatedPostManifests.add(manifest);
        K8sCluster updatedCluster = ImmutableK8sCluster
                .copyOf(cluster)
                .withPostManifests(updatedPostManifests);
        ClusterInventoryClient client = client();
        client.updateCluster(orgName, updatedCluster);
    }

    public List<String> postManifests()
            throws Exception
    {
        K8sCluster cluster = cluster();
        return Lists.newArrayList(cluster.postManifests());
    }

    // --------------------------------------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------------------------------------

    public void showClusterRequest()
            throws IOException
    {
        ImmutablesYamlMapper yamlMapper = new ImmutablesYamlMapper();
        String clusterRequestString = yamlMapper.write(clusterRequest());
        clusterRequestString = clusterRequestString.replaceAll("accessKey: \".*\"", "accessKey: \"***\"");
        clusterRequestString = clusterRequestString.replaceAll("secretKey: \".*\"", "secretKey: \"***\"");
        System.out.println(clusterRequestString);
    }

    public String clusterAlias()
    {
        return MapUtils.assertString(clusterRequest(), "alias");
    }

    public Map<String, Object> clusterRequest()
    {
        return context.variables().assertMap("clusterRequest");
    }

    private ClusterInventoryClient client()
    {
        return new ClusterInventoryClient(apiClient);
    }
}
