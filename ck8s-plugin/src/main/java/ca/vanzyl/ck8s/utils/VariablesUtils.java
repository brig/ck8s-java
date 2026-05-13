package ca.vanzyl.ck8s.utils;

import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VariablesUtils {

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

    public static String assertFile(Path workDir, Variables variables, String varName) {
        var path = assertPath(workDir, variables, varName);

        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load file '" + variables.assertString(varName) + "' (variable: '" + varName + "'): " + e.getMessage());
        }
    }

    public static byte[] assertBinaryFile(Path workDir, Variables variables, String varName) {
        var path = assertPath(workDir, variables, varName);

        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load file '" + variables.assertString(varName) + "' (variable: '" + varName + "'): " + e.getMessage());
        }
    }

    private static Path assertPath(Path workDir, Path path) {
        Path result = path.normalize().toAbsolutePath();
        if (!result.startsWith(workDir)) {
            throw new IllegalArgumentException("The path must be within the working directory: " + path);
        }
        return result;
    }

    private VariablesUtils() {
    }
}
