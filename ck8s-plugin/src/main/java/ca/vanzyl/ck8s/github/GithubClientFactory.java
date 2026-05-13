package ca.vanzyl.ck8s.github;

import org.eclipse.egit.github.core.RequestError;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class GithubClientFactory {

    public static GitHubClient create(String rawUrl) {
        String host;
        int port;
        String scheme;

        try {
            URI uri = new URI(rawUrl);
            host = uri.getHost();
            if ("github.com".equals(host) || "gist.github.com".equals(host)) {
                host = "api.github.com";
            }

            scheme = uri.getScheme();
            port = uri.getPort();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

        return new GitHubClient(host, port, scheme) {
            @Override
            protected IOException createException(InputStream response, int code, String status) {
                String responseBody = null;

                if (this.isError(code)) {
                    RequestError error;
                    try {
                        error = this.parseError(response);
                    } catch (IOException e) {
                        return e;
                    }

                    if (error != null) {
                        return new RequestException(error, code);
                    }
                } else {
                    try (BufferedInputStream reader = new BufferedInputStream(response)) {
                        responseBody = new String(reader.readAllBytes());
                    } catch (IOException e) {
                        // ignore
                    }
                }

                String message;
                if (status != null && !status.isEmpty()) {
                    message = status + " (" + code + ')';
                } else {
                    message = "Unknown error occurred (" + code + ')';
                }

                if (responseBody != null) {
                    message += "\n response: " + responseBody;
                }

                return new IOException(message);
            }
        };
    }
}
