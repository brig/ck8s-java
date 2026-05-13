package ca.vanzyl.ck8s.k8s.actions;

public final class K8sChangeType {

    public static final String NAMESPACE_TYPE = "k8s:namespace";
    public static final String LABEL_TYPE = "k8s:namespace:label";

    public static String namespaceId(String namespace) {
        return String.format("%s:%s", NAMESPACE_TYPE, namespace);
    }

    public static String labelId(String namespace, String label) {
        return String.format("%s:%s", namespaceId(namespace), label);
    }

    private K8sChangeType() {
    }
}
