package ca.vanzyl.ck8s.aws.cloudformation;

import ca.vanzyl.ck8s.aws.cloudformation.CloudFormationTaskParams.*;
import ca.vanzyl.ck8s.common.MapUtils;
import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.model.Capability;
import software.amazon.awssdk.services.cloudformation.model.ChangeSetType;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.ResourceToImport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ca.vanzyl.ck8s.common.MapUtils.assertNoNullValues;

public final class VariablesCloudFormationTaskParams {

    private static final String PROFILE_KEY = "profile";
    private static final String REGION_KEY = "region";
    private static final String STACK_NAME_KEY = "stackName";
    private static final String CAPABILITIES_KEY = "capabilities";
    private static final String LABELS_KEY = "labels";
    private static final String PARAMETERS_OVERRIDE_KEY = "parameterOverrides";
    private static final String CHANGE_SET_TYPE_KEY = "changeSetType";
    private static final String CHANGE_SET_NAME_KEY = "changeSetName";
    private static final String RESOURCES_TO_IMPORT_KEY = "resourcesToImport";

    public static DeployParams deploy(Variables variables) {
        return new DeployParams(
                baseParams(variables),
                assertStackName(variables),
                capabilities(variables),
                parameterOverrides(variables),
                assertTemplate(variables)
        );
    }

    public static DeleteParams delete(Variables variables) {
        return new DeleteParams(
                baseParams(variables),
                assertStackName(variables),
                new HashSet<>(variables.getList("retainResources", List.of()))
        );
    }

    public static CreateParams create(Variables variables) {
        return new CreateParams(
                baseParams(variables),
                assertStackName(variables),
                capabilities(variables),
                parameterOverrides(variables),
                assertTemplate(variables)
        );
    }

    public static CreateChangeSetParams createChangeSet(Variables variables) {
        var type = assertChangeSetType(variables.assertString(CHANGE_SET_TYPE_KEY));
        Collection<ResourceToImport> resourcesToImport = List.of();
        if (type == ChangeSetType.IMPORT) {
            resourcesToImport = assertResourcesToImport(variables);
        }

        return new CreateChangeSetParams(
                baseParams(variables),
                assertStackName(variables),
                assertChangeSetName(variables),
                assertChangeSetType(variables.assertString(CHANGE_SET_TYPE_KEY)),
                capabilities(variables),
                parameterOverrides(variables),
                assertTemplate(variables),
                resourcesToImport
        );
    }

    public static ExecuteChangeSetParams executeChangeSet(Variables variables) {
        return new ExecuteChangeSetParams(
                baseParams(variables),
                assertStackName(variables),
                assertChangeSetName(variables)
        );
    }

    public static ExistsParams exists(Variables variables) {
        return new ExistsParams(
                baseParams(variables),
                assertStackName(variables)
        );
    }

    public static GetTemplateParams getTemplate(Variables variables) {
        return new GetTemplateParams(
                baseParams(variables),
                assertStackName(variables)
        );
    }

    private static String assertChangeSetName(Variables variables) {
        return variables.assertString(CHANGE_SET_NAME_KEY);
    }

    private static List<Parameter> parameterOverrides(Variables variables) {
        return assertNoNullValues(variables.getMap(PARAMETERS_OVERRIDE_KEY, Map.of())).entrySet().stream()
                .map(entry -> Parameter.builder()
                        .parameterKey(entry.getKey().toString())
                        .parameterValue(entry.getValue().toString())
                        .build())
                .collect(Collectors.toList());
    }

    private static CloudFormationTaskParams.BaseParams baseParams(Variables variables) {
        return new CloudFormationTaskParams.BaseParams(profile(variables), assertRegion(variables));
    }

    private static String profile(Variables variables) {
        return variables.getString(PROFILE_KEY);
    }

    private static Region assertRegion(Variables variables) {
        return Region.of(variables.assertString(REGION_KEY));
    }

    private static String assertStackName(Variables variables) {
        return variables.assertString(STACK_NAME_KEY);
    }

    private static List<Capability> capabilities(Variables input) {
        var capabilities = input.assertVariable(CAPABILITIES_KEY, Object.class);
        if (capabilities instanceof List<?> l) {
            return l.stream()
                    .map(c -> assertCapability(c.toString()))
                    .toList();
        } else if (capabilities instanceof String s) {
            return List.of(assertCapability(s));
        } else {
            throw new UserDefinedException("Unsupported capability value type: " + capabilities.getClass() + ", expected string or list of strings");
        }
    }

    private static Capability assertCapability(String value) {
        var result = Capability.fromValue(value);
        if (result == null || result == Capability.UNKNOWN_TO_SDK_VERSION) {
            throw new UserDefinedException("Unknown capability: " + value + ", allowed values: " + Capability.knownValues());
        }
        return result;
    }

    private static Map<String, String> labels(Variables variables) {
        return variables.getMap(LABELS_KEY, Map.of()).entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toUnmodifiableMap(
                        entry -> String.valueOf(entry.getKey()),
                        entry -> String.valueOf(entry.getValue())
                ));
    }

    private static String assertTemplate(Variables input) {
        var templateFile = input.assertString("templateFile");

        String templateBody;
        try {
            templateBody = Files.readString(Paths.get(templateFile));
        } catch (IOException e) {
            throw new UserDefinedException("Can't load template file '" + templateFile + "': " + e.getMessage());
        }

        return templateBody;
    }

    private static ChangeSetType assertChangeSetType(String value) {
        var result = ChangeSetType.fromValue(value);
        if (result == null || result == ChangeSetType.UNKNOWN_TO_SDK_VERSION) {
            throw new UserDefinedException("Unknown change set type: " + value + ", allowed values: " + ChangeSetType.knownValues());
        }
        return result;
    }

    private static Collection<ResourceToImport> assertResourcesToImport(Variables variables) {
        return variables.<Map<String, Object>>assertList(RESOURCES_TO_IMPORT_KEY).stream()
                .map(resource -> ResourceToImport.builder()
                        .resourceType(MapUtils.assertString(resource, "resourceType"))
                        .logicalResourceId(MapUtils.assertString(resource, "logicalResourceId"))
                        .resourceIdentifier(MapUtils.assertMap(resource, "resourceIdentifier").entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())))
                        .build())
                .toList();
    }
}
