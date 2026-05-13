package ca.vanzyl.ck8s.inventory;

import com.walmartlabs.concord.client2.ApiClient;
import com.walmartlabs.concord.client2.InventoriesApi;
import com.walmartlabs.concord.client2.InventoryDataApi;
import com.walmartlabs.concord.client2.InventoryEntry;

import javax.inject.Named;
import java.util.Map;

@Named
public class ConcordInventoryClient
        extends ConcordClientSupport
{

    public ConcordInventoryClient(ApiClient apiClient)
    {
        super(apiClient);
    }

    public void createOrUpdate(String orgName, String inventoryName)
            throws Exception
    {

        new InventoriesApi(client)
                .createOrUpdateInventory(orgName, new InventoryEntry()
                        .name(inventoryName)
                        .orgName(orgName)
                        .visibility(InventoryEntry.VisibilityEnum.PUBLIC));
    }

    public void putItem(String orgName, String inventoryName, String id, String inventoryJson)
            throws Exception
    {
        InventoryDataApi dataApi = new InventoryDataApi(client);
        dataApi.updateInventoryData(orgName, inventoryName, inventoryName + "/" + id, inventoryJson);
    }

    public Map<String, Object> getItem(String orgName, String inventoryName, String id)
            throws Exception
    {
        InventoryDataApi dataApi = new InventoryDataApi(client);
        return (Map<String, Object>) dataApi.getInventoryData(orgName, inventoryName, inventoryName + "/" + id, true);
    }

    public Object getAllItems(String orgName, String inventoryName)
            throws Exception
    {
        InventoryDataApi dataApi = new InventoryDataApi(client);
        return dataApi.getInventoryData(orgName, inventoryName, inventoryName, false);
    }
}
