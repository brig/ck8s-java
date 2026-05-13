package ca.vanzyl.ck8s.k8s;

import ca.vanzyl.ck8s.actions.ActionUtils;
import ca.vanzyl.ck8s.actions.TaskActionExecutor;
import ca.vanzyl.ck8s.common.MapUtils;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Named("k8s")
@DryRunReady
public class K8sTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(K8sTask.class);

    private final List<K8sTaskAction<? extends K8sTaskParams>> actions;

    private final Context context;

    @Inject
    public K8sTask(List<K8sTaskAction<?>> actions, Context context) {
        this.actions = ActionUtils.assertActions(actions);
        this.context = context;
    }

    @Override
    @SensitiveData(keys = "data", includeNestedValues = true)
    public TaskResult execute(Variables input) throws Exception {
        return TaskActionExecutor.execute(context, input, K8sTaskAction.Action.class, actions, K8sTask::toActionInput);
    }

    public void patchEnvInKubeconfig(Map<String, Object> env, String envVariableName) throws IOException {
        var kubeconfig = MapUtils.getString(env, "KUBECONFIG");
        if (kubeconfig == null) {
            return;
        }

        var p = Path.of(kubeconfig);
        if (!Files.exists(p)) {
            log.warn("KUBECONFIG '{}' does not exist", kubeconfig);
            return;
        }

        var envValue = env.get(envVariableName);
        if (envValue == null) {
            return;
        }

        K8sClientFactory.patchEnvInKubeconfig(p, Map.of(envVariableName, envValue));

        log.info("KUBECONFIG '{}' env variables patched", kubeconfig);
    }

    private static K8sTaskParams toActionInput(K8sTaskAction.Action action, Variables variables) {
        return switch (action) {
            case CREATE_NAMESPACE -> VariablesK8sTaskParams.createNamespace(variables);
            case DELETE_NAMESPACE -> VariablesK8sTaskParams.deleteNamespace(variables);
            case NAMESPACE_EXISTS -> VariablesK8sTaskParams.existsNamespace(variables);
            case GET_SECRET_DATA -> VariablesK8sTaskParams.getSecret(variables);
            case UPSERT_SECRET -> VariablesK8sTaskParams.createSecret(variables);
            case GET_PODS -> VariablesK8sTaskParams.getPods(variables);
            case LIST_EVENTS -> VariablesK8sTaskParams.listEvents(variables);
            case POD_LOGS -> VariablesK8sTaskParams.podLogs(variables);
        };
    }
}
