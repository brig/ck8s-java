package ca.vanzyl.ck8s.aws.asm;

import ca.vanzyl.ck8s.actions.ActionInput;
import software.amazon.awssdk.regions.Region;

import java.util.Map;

public interface AsmTaskParams extends ActionInput {

    BaseParams baseParams();

    record BaseParams(String profile, Region region) {
    }

    record CreateSecretParams(
            BaseParams baseParams,
            String name,
            String secretString,
            Map<String, String> tags
    ) implements AsmTaskParams {
    }

    record GetSecretParams(
            BaseParams baseParams,
            String name
    ) implements AsmTaskParams {
    }

    record UpdateSecretParams(
            BaseParams baseParams,
            String name,
            String secretString
    ) implements AsmTaskParams {
    }

    record DeleteSecretParams(
            BaseParams baseParams,
            String name
    ) implements AsmTaskParams {
    }
}
