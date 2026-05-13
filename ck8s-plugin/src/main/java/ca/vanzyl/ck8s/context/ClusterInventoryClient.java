package ca.vanzyl.ck8s.context;

import ca.vanzyl.ck8s.inventory.ConcordClientSupport;
import ca.vanzyl.ck8s.inventory.ConcordInventoryClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.walmartlabs.concord.client2.ApiClient;
import com.walmartlabs.concord.client2.ApiException;

import javax.inject.Named;
import java.util.Map;

@Named
public class ClusterInventoryClient
        extends ConcordClientSupport
{

    private final static String INVENTORY_NAME = "k8sClusters";
    private final ConcordInventoryClient inventory;
    private final ObjectMapper mapper;
    private final Configurator configurator;

    public ClusterInventoryClient(ApiClient apiClient)
    {
        super(apiClient);
        this.inventory = new ConcordInventoryClient(apiClient);
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new GuavaModule());
        this.configurator = new Configurator();
    }

    public void createInventory(String orgName)
            throws Exception
    {
        inventory.createOrUpdate(orgName, INVENTORY_NAME);
    }

    public void updateCluster(String orgName, K8sCluster cluster)
            throws Exception
    {
        inventory.putItem(orgName, INVENTORY_NAME, cluster.id(), mapper.writeValueAsString(cluster));
    }

    public K8sCluster getCluster(String orgName, String clusterId)
            throws Exception
    {
        Map<String, Object> clusterAsMap = inventory.getItem(orgName, INVENTORY_NAME, clusterId);
        return configurator.createConfiguration(clusterAsMap, K8sCluster.class);
    }

    public boolean clusterExists(String orgName, String clusterId)
            throws Exception
    {
        Map<String, Object> clusterAsMap;
        try {
            clusterAsMap = inventory.getItem(orgName, INVENTORY_NAME, clusterId);
        }
        catch (ApiException e) {
            // The inventory doesn't exist yet, so let's create it and return false as we have
            // no entry for this cluster.
            createInventory(orgName);
            return false;
        }
        return clusterAsMap != null;
    }

    public K8sClusters getClusters(String orgName)
            throws Exception
    {
        return mapper.readValue(mapper.writeValueAsString(inventory.getAllItems(orgName, INVENTORY_NAME)), K8sClusters.class);
    }
}
