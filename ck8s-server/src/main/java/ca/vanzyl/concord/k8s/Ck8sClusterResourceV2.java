package ca.vanzyl.concord.k8s;

import com.walmartlabs.concord.server.GenericOperationResult;
import com.walmartlabs.concord.server.OperationResult;
import com.walmartlabs.concord.server.sdk.metrics.WithTimer;
import com.walmartlabs.concord.server.sdk.rest.Resource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Deprecated
@Named
@Singleton
@Path("/api/ck8s/v2/cluster")
public class Ck8sClusterResourceV2 implements Resource {

    private final Ck8sProcessResourceV2.ConnectedClustersDao dao;

    @Inject
    public Ck8sClusterResourceV2(Ck8sProcessResourceV2.ConnectedClustersDao dao) {
        this.dao = dao;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WithTimer
    public GenericOperationResult registerCluster(Ck8sProcessResourceV2.ClusterInfo clusterInfo) {
        dao.registerCluster(clusterInfo);
        return new GenericOperationResult(OperationResult.UPDATED);
    }

    @DELETE
    @Path("/{clusterAlias}")
    @Produces(MediaType.APPLICATION_JSON)
    public GenericOperationResult unregisterCluster(@PathParam("clusterAlias") String clusterAlias) {
        dao.unregisterCluster(clusterAlias);
        return new GenericOperationResult(OperationResult.DELETED);
    }
}
