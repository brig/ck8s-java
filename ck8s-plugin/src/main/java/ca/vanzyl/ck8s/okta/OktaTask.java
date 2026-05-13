package ca.vanzyl.ck8s.okta;

import ca.vanzyl.ck8s.common.Mapper;
import ca.vanzyl.ck8s.utils.ClientUtils;
import com.walmartlabs.concord.client2.ApiException;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

import static ca.vanzyl.ck8s.utils.ClientUtils.apiException;

@Named("okta")
@DryRunReady
public class OktaTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(OktaTask.class);

    private static final int DEFAULT_CONNECT_TIMEOUT = 30;
    private static final int DEFAULT_REQUEST_TIMEOUT = 30;

    private static final int REQUEST_RETRY_COUNT = 5;
    private static final long REQUEST_RETRY_INTERVAL = 15000;

    private final boolean dryRunMode;

    @Inject
    public OktaTask(Context context) {
        this.dryRunMode = context.processConfiguration().dryRun();
    }

    @Override
    public TaskResult execute(Variables input) throws Exception {
        var action = input.assertString("action");
        if ("get-app".equals(action)) {
            return getApp(input);
        } else if ("update-app".equals(action)) {
            return updateApp(input);
        }
        throw new IllegalArgumentException("Unsupported action: " + action);
    }

    private TaskResult getApp(Variables input) {
        var oktaApiToken = input.assertString("apiToken");
        var url = input.assertString("oauthBaseUrl");
        var clientId = input.assertString("clientId");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/v1/apps/%s", url, clientId)))
                .header("Authorization", "SSWS " + oktaApiToken)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(input.getInt("requestTimeout", DEFAULT_REQUEST_TIMEOUT)))
                .GET()
                .build();

        var client = client(input);
        try {
            var response = send(client, request);
            var app = Mapper.json().readMap(response);
            return TaskResult.success()
                    .value("app", app);
        } catch (Exception e) {
            return TaskResult.fail(e);
        }
    }

    private TaskResult updateApp(Variables input) {
        var oktaApiToken = input.assertString("apiToken");
        var url = input.assertString("oauthBaseUrl");
        var clientId = input.assertString("clientId");
        var app = input.assertMap("app");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/v1/apps/%s", url, clientId)))
                .header("Authorization", "SSWS " + oktaApiToken)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(input.getInt("requestTimeout", DEFAULT_REQUEST_TIMEOUT)))
                .PUT(HttpRequest.BodyPublishers.ofString(Mapper.json().writeAsString(app)))
                .build();

        var client = client(input);

        if (dryRunMode) {
            log.info("Running in dry-run mode: skipping updating app");
            return TaskResult.success();
        }

        try {
            var response = send(client, request);
            return TaskResult.success()
                    .value("response", Mapper.json().readMap(response));
        } catch (Exception e) {
            return TaskResult.fail(e);
        }
    }

    private String send(HttpClient client, HttpRequest request) throws ApiException {
        return withRetry(() -> {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw apiException(response);
            }
            return response.body();
        });
    }

    private static <T> T withRetry(Callable<T> c) throws ApiException {
        // retry 403 also, sometimes okta returns 403 error...
        return ClientUtils.withRetry(REQUEST_RETRY_COUNT, REQUEST_RETRY_INTERVAL, List.of(403), c);
    }

    private static HttpClient client(Variables input) {
        var connectTimeout = Duration.ofSeconds(input.getInt("connectTimeout", DEFAULT_CONNECT_TIMEOUT));

        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectTimeout)
                .build();
    }
}
