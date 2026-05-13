package ca.vanzyl.ck8s.aws.cognito.actions;

import ca.vanzyl.ck8s.aws.AwsTaskUtils;
import ca.vanzyl.ck8s.aws.cognito.CognitoClientFactory;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskAction;
import ca.vanzyl.ck8s.aws.cognito.CognitoTaskParams;
import com.google.inject.Inject;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsRequest;

public class ListUserPoolsAction extends CognitoTaskAction<CognitoTaskParams.ListUserPoolsParams> {

    @Inject
    public ListUserPoolsAction(CognitoClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.LIST_USER_POOLS;
    }

    @Override
    public TaskResult execute(Context context, CognitoTaskParams.ListUserPoolsParams input) throws Exception {
        var maxResults = input.maxResults();
        try (var client = createClient(input)) {
            var userPools = client.listUserPoolsPaginator(
                            ListUserPoolsRequest.builder()
                                    .maxResults(maxResults)
                                    .build()).stream()
                    .flatMap(response -> response.userPools().stream())
                    .map(AwsTaskUtils::serialize)
                    .toList();

            return TaskResult.success()
                    .value("pools", userPools);
        }
    }
}
