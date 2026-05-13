package ca.vanzyl.ck8s.aws.asm;

import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import software.amazon.awssdk.regions.Region;

import java.util.Map;
import java.util.stream.Collectors;

public final class VariablesAsmTaskParams {

    public static AsmTaskParams.CreateSecretParams createSecret(Variables variables) {
        return new AsmTaskParams.CreateSecretParams(
                baseParams(variables),
                variables.assertString("name"),
                variables.assertString("secretString"),
                getTags(variables)
        );
    }

    public static AsmTaskParams.GetSecretParams getSecret(Variables variables) {
        return new AsmTaskParams.GetSecretParams(
                baseParams(variables),
                variables.assertString("name")
        );
    }

    public static AsmTaskParams.UpdateSecretParams updateSecret(Variables variables) {
        return new AsmTaskParams.UpdateSecretParams(
                baseParams(variables),
                variables.assertString("name"),
                variables.assertString("secretString")
        );
    }

    public static AsmTaskParams.DeleteSecretParams deleteSecret(Variables variables) {
        return new AsmTaskParams.DeleteSecretParams(
                baseParams(variables),
                variables.assertString("name")
        );
    }

    private static AsmTaskParams.BaseParams baseParams(Variables variables) {
        return new AsmTaskParams.BaseParams(
                variables.getString("profile"),
                Region.of(variables.assertString("region"))
        );
    }

    private static Map<String, String> getTags(Variables variables) {
        return variables.<String, Object>getMap("tags", Map.of()).entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toUnmodifiableMap(
                        entry -> String.valueOf(entry.getKey()),
                        entry -> String.valueOf(entry.getValue())
                ));
    }
}