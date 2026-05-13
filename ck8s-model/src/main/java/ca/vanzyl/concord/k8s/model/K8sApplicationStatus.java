package ca.vanzyl.concord.k8s.model;

public enum K8sApplicationStatus
{
    /**
     * Initial status. Nothing happened yet.
     */
    UNKNOWN,

    /**
     * Application has been provisioned and should be up and running.
     */
    READY,

    /**
     * Application is being updated.
     */
    UPDATING,

    /**
     * Application is being removed.
     */
    DELETING,

    /**
     * Application has been removed.
     */
    DELETED
}
