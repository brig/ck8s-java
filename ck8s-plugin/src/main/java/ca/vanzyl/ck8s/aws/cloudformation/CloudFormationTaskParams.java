package ca.vanzyl.ck8s.aws.cloudformation;

import ca.vanzyl.ck8s.actions.ActionInput;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.model.Capability;
import software.amazon.awssdk.services.cloudformation.model.ChangeSetType;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.ResourceToImport;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface CloudFormationTaskParams extends ActionInput {

    BaseParams baseParams();

    record BaseParams(String profile, Region region) {
    }

    record DeployParams(
            BaseParams baseParams,
            String stackName,
            List<Capability> capabilities,
            List<Parameter> parameterOverrides,
            String templateBody
    ) implements CloudFormationTaskParams {
    }

    record DeleteParams(BaseParams baseParams, String stackName, Set<String> retainResources) implements CloudFormationTaskParams {
    }

    record CreateParams(
            BaseParams baseParams,
            String stackName,
            List<Capability> capabilities,
            List<Parameter> parameterOverrides,
            String templateBody
    ) implements CloudFormationTaskParams {
    }

    record ExistsParams(BaseParams baseParams, String stackName) implements CloudFormationTaskParams {
    }

    record GetTemplateParams(BaseParams baseParams, String stackName) implements CloudFormationTaskParams {
    }

    record CreateChangeSetParams(
            BaseParams baseParams,
            String stackName,
            String changeSetName,
            ChangeSetType changeSetType,
            List<Capability> capabilities,
            List<Parameter> parameterOverrides,
            String templateBody,
            Collection<ResourceToImport> resourcesToImport
    ) implements CloudFormationTaskParams {
    }

    record ExecuteChangeSetParams(
            BaseParams baseParams,
            String stackName,
            String changeSetName
    ) implements CloudFormationTaskParams {
    }
}
