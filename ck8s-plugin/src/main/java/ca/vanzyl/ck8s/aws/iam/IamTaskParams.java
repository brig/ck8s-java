package ca.vanzyl.ck8s.aws.iam;

import ca.vanzyl.ck8s.actions.ActionInput;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.model.Tag;

import java.util.List;

public interface IamTaskParams extends ActionInput {

    BaseParams baseParams();

    record BaseParams(String profile, Region region, boolean debug) {
    }

    record CreateRoleParams(
            BaseParams baseParams,
            String roleName,
            String trustPolicy,
            List<Tag> tags
    ) implements IamTaskParams {
    }

    record DeleteRoleParams(
            BaseParams baseParams,
            String roleName
    ) implements IamTaskParams {
    }

    record GetRoleParams(
            BaseParams baseParams,
            String roleName
    ) implements IamTaskParams {
    }

    record ListRolesParams(
            BaseParams baseParams
    ) implements IamTaskParams {
    }

    record PutRolePolicyParams(
            BaseParams baseParams,
            String roleName,
            String policyName,
            String policyDocument
    ) implements IamTaskParams {
    }

    record CreatePolicyParams(
            BaseParams baseParams,
            String policyName,
            String policyArn,
            String policyDocument
    ) implements IamTaskParams {
    }

    record DeletePolicyParams(
            BaseParams baseParams,
            String policyArn,
            boolean detachFromResources
    ) implements IamTaskParams {
    }

    record ListPoliciesParams(
            BaseParams baseParams
    ) implements IamTaskParams {
    }

    record AttachPolicyParams(
            BaseParams baseParams,
            String roleName,
            String policyName,
            String policyArn
    ) implements IamTaskParams {
    }
}
