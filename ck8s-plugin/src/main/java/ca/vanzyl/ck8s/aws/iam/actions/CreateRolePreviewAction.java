package ca.vanzyl.ck8s.aws.iam.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.iam.IamClientFactory;
import ca.vanzyl.ck8s.aws.iam.IamTaskAction;
import ca.vanzyl.ck8s.aws.iam.state.IamRole;
import ca.vanzyl.ck8s.aws.iam.state.IamState;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.model.Tag;

import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;

import static ca.vanzyl.ck8s.aws.iam.IamTaskParams.CreateRoleParams;
import static ca.vanzyl.ck8s.aws.iam.actions.CreateRoleAction.dumpInput;

public class CreateRolePreviewAction extends IamTaskAction<CreateRoleParams> {

    private final static Logger log = LoggerFactory.getLogger(CreateRolePreviewAction.class);

    private final IamState state;

    @Inject
    public CreateRolePreviewAction(IamClientFactory clientFactory, IamState state) {
        super(clientFactory);
        this.state = state;
    }

    @Override
    public Action action() {
        return Action.CREATE_ROLE;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.PREVIEW);
    }

    @Override
    public TaskResult execute(Context context, CreateRoleParams input) throws Exception {
        var roleName = input.roleName();
        var trustPolicy = input.trustPolicy();

        dumpInput(input);

        // just to load current
        var role = state.role(input.baseParams(), roleName);
        if (role == null) {
            log.info("[PREVIEW] Role '{}' does not exists. Creating it...", roleName);
            state.put(IamRole.builder()
                    .roleName(roleName)
                    .trustPolicy(trustPolicy)
                    .tags(input.tags().stream().collect(Collectors.toMap(Tag::key, Tag::value)))
                    .build());
        } else {
            log.info("[PREVIEW] Role '{}' exists. Updating it...", roleName);
            state.put(IamRole.builder().from(role)
                    .roleName(roleName)
                    .trustPolicy(trustPolicy)
                    .tags(input.tags().stream().collect(Collectors.toMap(Tag::key, Tag::value)))
                    .build());
        }

        return TaskResult.success();
    }
}
