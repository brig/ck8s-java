package ca.vanzyl.ck8s.k8s.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.k8s.K8sClientFactory;
import ca.vanzyl.ck8s.k8s.K8sTaskAction;
import ca.vanzyl.ck8s.k8s.K8sTaskParams;
import ca.vanzyl.ck8s.preview.Change;
import ca.vanzyl.ck8s.preview.PreviewChangesRecorder;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;

public class DeleteNamespacePreviewAction implements K8sTaskAction<K8sTaskParams.DeleteNamespaceParams> {

    private static final Logger log = LoggerFactory.getLogger(DeleteNamespacePreviewAction.class);

    private final PreviewChangesRecorder preview;

    @Inject
    public DeleteNamespacePreviewAction(PreviewChangesRecorder preview) {
        this.preview = preview;
    }

    @Override
    public Action action() {
        return Action.DELETE_NAMESPACE;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.PREVIEW);
    }

    @Override
    public TaskResult execute(Context context, K8sTaskParams.DeleteNamespaceParams input) throws Exception {
        var namespace = input.namespace();

        try (var client = K8sClientFactory.create(input)) {
            var existingNamespace = client.namespaces().withName(namespace).get();

            if (existingNamespace != null) {
                preview.record(c -> c.action(Change.Action.DELETE)
                        .id(K8sChangeType.namespaceId(namespace))
                        .type(K8sChangeType.NAMESPACE_TYPE)
                        .metadata(Change.Metadata.builder().name(namespace).build()));
            }

            return TaskResult.success();
        } catch (KubernetesClientException e) {
            log.error("❌ Failed to preview delete namespace '{}': {}", namespace, e.getMessage());
            return TaskResult.fail(e);
        }
    }
}
