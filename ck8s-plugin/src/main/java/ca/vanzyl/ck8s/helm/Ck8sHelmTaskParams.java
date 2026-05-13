package ca.vanzyl.ck8s.helm;

import ca.vanzyl.ck8s.actions.ActionInput;

import java.nio.file.Path;
import java.util.Map;

public sealed interface Ck8sHelmTaskParams extends ActionInput {

    BaseParams baseParams();

    record BaseParams(String helm, Map<String, String> env, Path pwd, boolean debug, Long timeout) {
    }

    record Status(
            BaseParams baseParams,
            String namespace,
            String release,
            boolean silent
    ) implements Ck8sHelmTaskParams {
    }
}
