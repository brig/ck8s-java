package ca.vanzyl.ck8s.inventory;

import com.walmartlabs.concord.client2.ApiClient;

public class ConcordClientSupport
{
    protected final ApiClient client;

    public ConcordClientSupport(ApiClient apiClient)
    {
        this.client = apiClient;
    }
}
