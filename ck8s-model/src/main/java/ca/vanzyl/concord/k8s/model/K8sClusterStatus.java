package ca.vanzyl.concord.k8s.model;

public enum K8sClusterStatus
{
    /**
     * Initial status. Nothing happened yet.
     */
    UNKNOWN,

    /**
     * Initial provisioning of the cluster.
     * TODO(ib)
     */
    PROVISIONING,

    /**
     * There's an active operation running associated with the cluster.
     */
    UPDATING,

    /**
     * Ready to rumble.
     */
    READY,

    /**
     * Cluster removal is in progress.
     */
    DESTROYING,

    /**
     * Cluster was removed.
     * TODO(ib)
     */
    DESTROYED
}
