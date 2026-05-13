package ca.vanzyl.ck8s.k8s.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.k8s.K8sClientFactory;
import ca.vanzyl.ck8s.k8s.K8sTaskAction;
import ca.vanzyl.ck8s.k8s.K8sTaskParams;
import ca.vanzyl.ck8s.preview.Change;
import ca.vanzyl.ck8s.preview.PreviewChangesRecorder;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;

public class CreateNamespacePreviewAction implements K8sTaskAction<K8sTaskParams.CreateNamespaceParams> {

    private static final Logger log = LoggerFactory.getLogger(CreateNamespacePreviewAction.class);

    private final PreviewChangesRecorder preview;

    @Inject
    public CreateNamespacePreviewAction(PreviewChangesRecorder preview) {
        this.preview = preview;
    }

    @Override
    public Action action() {
        return Action.CREATE_NAMESPACE;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.PREVIEW);
    }

    @Override
    public TaskResult execute(Context context, K8sTaskParams.CreateNamespaceParams input) throws Exception {
        var namespace = input.namespace();
        var labels = input.labels();

        try (var client = K8sClientFactory.create(input)) {
            var existingNamespace = client.namespaces().withName(namespace).get();

            if (existingNamespace != null) {
                if (!labels.isEmpty()) {
                    var existingLabels = Optional.of(existingNamespace)
                            .map(Namespace::getMetadata)
                            .map(ObjectMeta::getLabels)
                            .orElseGet(Map::of);

                    if (!existingLabels.entrySet().containsAll(labels.entrySet())) {
                        preview.record(c -> c.action(Change.Action.UPDATE)
                                .id(K8sChangeType.namespaceId(namespace))
                                .type(K8sChangeType.NAMESPACE_TYPE)
                                .metadata(Change.Metadata.builder().name(namespace).build()));

                        preview.record(labelsToChanges(namespace, existingLabels, labels));
                    }
                }
            } else {
                preview.record(c -> c.action(Change.Action.CREATE)
                        .id(K8sChangeType.namespaceId(namespace))
                        .type(K8sChangeType.NAMESPACE_TYPE)
                        .metadata(Change.Metadata.builder().name(namespace).build()));

                preview.record(labelsToChanges(namespace, Map.of(), labels));
            }

            return TaskResult.success();
        } catch (KubernetesClientException e) {
            log.error("❌ Failed to preview create namespace '{}': {}", namespace, e.getMessage());
            return TaskResult.fail(e);
        }
    }

    private List<Change> labelsToChanges(String namespace, Map<String, String> currentLabels, Map<String, String> newLabels) {
        List<Change> changes = new ArrayList<>();

        for (var entry : newLabels.entrySet()) {
            var key = entry.getKey();
            var newValue = entry.getValue();
            var currentValue = currentLabels.get(key);

            Change.Action changeAction = null;
            if (currentValue == null) {
                changeAction = Change.Action.CREATE;
            } else if (!currentValue.equals(newValue)) {
                changeAction = Change.Action.UPDATE;
            }

            if (changeAction != null) {
                changes.add(Change.builder()
                        .action(changeAction)
                        .id(K8sChangeType.labelId(namespace, key))
                        .parentId(K8sChangeType.namespaceId(namespace))
                        .type(K8sChangeType.LABEL_TYPE)
                        .metadata(Change.Metadata.builder().name(key).build())
                        .build());
            }
        }
        return changes;
    }
}
