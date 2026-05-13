package ca.vanzyl.ck8s.helm.actions;

import ca.vanzyl.ck8s.helm.Ck8sHelmTaskAction;
import ca.vanzyl.ck8s.helm.Ck8sHelmTaskParams;
import ca.vanzyl.ck8s.helm.Command;
import ca.vanzyl.ck8s.helm.JsonMapper;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class StatusAction extends Ck8sHelmTaskAction<Ck8sHelmTaskParams.Status> {

    @Override
    public TaskResult execute(Context context, Ck8sHelmTaskParams.Status input) throws Exception {
        var cmd = List.of(
                input.baseParams().helm(), "status", "--output", "json", "--namespace", input.namespace(), input.release()
        );

        var result = new Command(input.baseParams().pwd(), input.baseParams().env())
                .execute(cmd, input.baseParams().timeout(), TimeUnit.SECONDS, input.silent(), input.baseParams().debug());

        if (result.code() == 0) {
            var raw = JsonMapper.asMap(result.stdout());

            return TaskResult.success()
                    .value("exists", true)
                    .value("status", 0)
                    .value("deployed", isDeployed(raw))
                    .value("version", version(raw));
        } else {
            return TaskResult.success()
                    .value("exists", false)
                    .value("status", result.code());
        }
    }

    private static boolean isDeployed(Map<String, Object> raw) {
        var info = raw.get("info");
        if (!(info instanceof Map<?,?> m)) {
            return false;
        }

        var st = m.get("status");
        if (st instanceof String s) {
            return "deployed".equalsIgnoreCase(s);
        }

        if (st instanceof Map<?,?> mm) {
            var s = mm.get("status");
            return s instanceof String ss && "deployed".equalsIgnoreCase(ss);
        }

        return false;
    }

    private static Integer version(Map<String, Object> raw) {
        var v = raw.get("version");
        if (v == null) {
            return null;
        }

        if (v instanceof Number n) {
            return n.intValue();
        }

        try {
            return Integer.valueOf(String.valueOf(v));
        } catch (NumberFormatException ignore) {
            return null;
        }
    }
}
