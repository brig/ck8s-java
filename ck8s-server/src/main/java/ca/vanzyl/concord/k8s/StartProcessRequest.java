package ca.vanzyl.concord.k8s;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Deprecated
public interface StartProcessRequest {

    String getClusterAlias();

    default boolean isBuildClusterRequest() {
        return true;
    }

    default String getClientClusterAlias() {
        return null;
    }

    default String anyClusterAlias() {
        return null;
    }

    String getOrg();

    String getProject();

    default boolean hasFlowsArchive() {
        return false;
    }

    default InputStream getFlowsArchive() {
        throw new IllegalStateException("Not implemented");
    }

    default InputStream getAdditionalConcordYaml() {
        throw new IllegalStateException("Not implemented");
    }

    default Map<String, InputPart> getAttachments() {
        return Map.of();
    }

    default String getAdditionalConcordYamlName() {
        throw new IllegalStateException("Not implemented");
    }

    default boolean hasAdditionalConcordYaml() {
        return false;
    }

    default String getCk8sRef() {
        return null;
    }

    default String getCk8sExtRef() {
        return null;
    }

    default boolean isIncludeTests() {
        return false;
    }

    default Map<String, ? extends Object> getArguments() {
        return Map.of();
    }

    default boolean isDebug() {
        return false;
    }

    default Map<String, ? extends Object> getMeta() {
        return Map.of();
    }

    String getFlowName();

    default Map<String, Object> getRequirements() {
        return Map.of();
    }

    default Map<String, Object> getExclusiveOptions() {
        return Map.of();
    }

    default UUID getParentInstanceId() {
        return null;
    }

    default List<String> getActiveProfiles() {
        return List.of();
    }

    default List<String> getOutExpressions() {
        return List.of();
    }

    default boolean isDryRun() {
        return false;
    }
}
