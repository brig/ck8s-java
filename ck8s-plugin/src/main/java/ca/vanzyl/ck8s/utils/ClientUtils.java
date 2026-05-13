package ca.vanzyl.ck8s.utils;

import com.walmartlabs.concord.client2.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.Callable;

public final class ClientUtils {

    private static final Logger log = LoggerFactory.getLogger(ClientUtils.class);

    public static <T> T withRetry(int retryCount, long retryInterval, Callable<T> c) throws ApiException {
        return withRetry(retryCount, retryInterval, List.of(), c);
    }

    public static <T> T withRetry(int retryCount, long retryInterval, List<Integer> retryCodes, Callable<T> c) throws ApiException {
        Exception exception = null;
        int tryCount = 0;
        while (!Thread.currentThread().isInterrupted() && tryCount < retryCount + 1) {
            try {
                return c.call();
            } catch (ApiException e) {
                exception = e;

                if (e.getCode() == 429) {
                    String retryAfter = e.getResponseHeaders().firstValue("Retry-After").orElse("1");
                    int retryAfterSeconds = Integer.parseInt(retryAfter);
                    log.info("Rate limit exceeded. Retrying in {} seconds...", retryAfterSeconds);
                    sleep(retryAfterSeconds * 1000L);
                    continue;
                }

                if (e.getCode() >= 400 && e.getCode() < 500 && !retryCodes.contains(e.getCode())) {
                    break;
                }

                log.warn("call error: '{}'", getErrorMessage(e));
            } catch (Exception e) {
                exception = e;
                log.error("call error", e);
            }
            log.info("retry after {} sec", retryInterval / 1000);
            sleep(retryInterval);
            tryCount++;
        }

        if (exception instanceof ApiException) {
            throw (ApiException) exception;
        }

        throw new ApiException(exception);
    }

    public static ApiException apiException(HttpResponse<String> response) {
        String body = response.body() == null ? null : response.body();
        String message = formatExceptionMessage(response.statusCode(), body);
        return new ApiException(response.statusCode(), message, response.headers(), body);
    }

    private static String formatExceptionMessage(int statusCode, String body) {
        if (body == null || body.isEmpty()) {
            body = "[no body]";
        }
        return " call failed with: " + statusCode + " - " + body;
    }

    private static String getErrorMessage(ApiException e) {
        String error = e.getMessage();
        if (e.getResponseBody() != null && !e.getResponseBody().isEmpty()) {
            error += ": " + e.getResponseBody();
        }
        return error;
    }

    private static void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private ClientUtils() {
    }
}
