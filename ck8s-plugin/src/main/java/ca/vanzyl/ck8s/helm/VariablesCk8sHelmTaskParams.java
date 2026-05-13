package ca.vanzyl.ck8s.helm;

import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class VariablesCk8sHelmTaskParams {

    @SuppressWarnings("unchecked")
    public static <E extends Ck8sHelmTaskParams> E params(Ck8sHelmTaskAction.Action action, Context context, Variables variables) {
        return switch (action) {
            case STATUS -> (E)status(context, variables);
        };
    }

    public static Ck8sHelmTaskParams.Status status(Context context, Variables variables) {
        return new Ck8sHelmTaskParams.Status(
                baseParams(context, variables),
                assertNamespace(variables),
                assertRelease(variables),
                variables.getBoolean("silent", false)
        );
    }

    private static Ck8sHelmTaskParams.BaseParams baseParams(Context context, Variables variables) {
        return new  Ck8sHelmTaskParams.BaseParams(
                variables.getString("helmPath", "helm"),
                env(variables),
                workDir(context, variables),
                variables.getBoolean("debug", context.processConfiguration().debug()),
                Optional.ofNullable(variables.getNumber("commandTimeout", null))
                        .map(Number::longValue)
                        .orElse(null)
        );
    }

    private static Path workDir(Context context, Variables variables) {
        var dir = variables.getString("workDir");
        if (dir == null) {
            return context.workingDirectory();
        }

        var path = Path.of(dir);
        if (!Files.exists(path)) {
            path = context.workingDirectory().resolve(path);
        }

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Failed to determine workdir '" + dir + "': not found");
        }

        return assertPath(context.workingDirectory(), path);
    }

    private static String assertNamespace(Variables variables) {
        return variables.assertString("namespace");
    }

    private static String assertRelease(Variables variables) {
        return variables.assertString("release");
    }

    private static Map<String, String> env(Variables variables) {
        return variables.getMap("env", Map.of()).entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toUnmodifiableMap(
                        entry -> String.valueOf(entry.getKey()),
                        entry -> String.valueOf(entry.getValue())
                ));
    }


    public static Path assertPath(Path workDir, Variables variables, String varName) {
        var fileName = variables.assertString(varName);

        var path = Path.of(fileName);
        if (!Files.exists(path)) {
            path = workDir.resolve(path);
        }

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Failed to load file '" + fileName + "' (variable: '" + varName + "'): file not found");
        }

        return assertPath(workDir, path);
    }

    private static Path assertPath(Path workDir, Path path) {
        Path result = path.normalize().toAbsolutePath();
        if (!result.startsWith(workDir)) {
            throw new IllegalArgumentException("The path must be within the working directory: " + path);
        }
        return result;
    }
}
