package ca.vanzyl.ck8s.aws.cognito;

import ca.vanzyl.ck8s.actions.ActionUtils;
import ca.vanzyl.ck8s.actions.TaskActionExecutor;
import com.walmartlabs.concord.runtime.v2.sdk.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@Named("ck8sCognito")
@DryRunReady
public class CognitoTask implements Task {

    private final List<CognitoTaskAction<? extends CognitoTaskParams>> actions;
    private final Context context;

    @Inject
    public CognitoTask(List<CognitoTaskAction<?>> actions, Context context) {
        this.actions = ActionUtils.assertActions(actions);
        this.context = context;
    }

    @Override
    @SensitiveData(keys = "clientSecret")
    public TaskResult execute(Variables input) throws Exception {
        return TaskActionExecutor.execute(context, input, CognitoTaskAction.Action.class, actions, this::toActionInput);
    }

    private CognitoTaskParams toActionInput(CognitoTaskAction.Action action, Variables variables) {
        return switch (action) {
            case UPSERT_USER_POOL, VERIFY_USER_POOL -> VariablesCognitoTaskParams.createUserPoolParams(context, variables);
            case DELETE_USER_POOLS -> VariablesCognitoTaskParams.deleteUserPools(variables);
            case DELETE_USER_POOL -> VariablesCognitoTaskParams.deleteUserPool(variables);
            case LIST_USER_POOLS -> VariablesCognitoTaskParams.listUserPools(variables);
            case FIND_USER_POOL -> VariablesCognitoTaskParams.findUserPool(variables);
            case GET_USER_POOL_CLIENT -> VariablesCognitoTaskParams.getUserPoolClient(variables);
            case FIND_USER_POOL_CLIENT -> VariablesCognitoTaskParams.findUserPoolClient(variables);
            case UPSERT_IDENTITY_PROVIDER, VERIFY_IDENTITY_PROVIDER -> VariablesCognitoTaskParams.createIdentityProvider(variables);
            case UPSERT_RESOURCE_SERVER, VERIFY_RESOURCE_SERVER -> VariablesCognitoTaskParams.createResourceServer(variables);
            case UPSERT_USER_POOL_CLIENT, VERIFY_USER_POOL_CLIENT -> VariablesCognitoTaskParams.createUserPoolClient(variables);
            case UPSERT_USER_POOL_DOMAIN, VERIFY_USER_POOL_DOMAIN -> VariablesCognitoTaskParams.createUserPoolDomain(variables);
            case UPSERT_USER_POOL_UI_CUSTOMIZATION, VERIFY_USER_POOL_UI_CUSTOMIZATION -> VariablesCognitoTaskParams.createUserPoolUICustomization(context.workingDirectory(), variables);
            case CREATE_USER_POOL_USER, VERIFY_USER_POOL_USER -> VariablesCognitoTaskParams.addUserPoolUser(variables);
            case UPSERT_USER_POOL_CLIENT_CALLBACKS, VERIFY_USER_POOL_CLIENT_CALLBACKS -> VariablesCognitoTaskParams.createUserPoolClientCallbacks(variables);
        };
    }
}
