package ca.vanzyl.concord.k8s;

import com.walmartlabs.concord.server.sdk.rest.Resource;
import org.eclipse.jetty.util.IO;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import javax.inject.Named;
import javax.inject.Singleton;

import static java.util.Objects.requireNonNull;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Named
@Singleton
@Path("/api/ck8s/v1/mica/view")
public class Ck8sMicaViewResource implements Resource {

    private final HttpClient client;
    private final URI micaUri;
    private final String micaApiKey;

    public Ck8sMicaViewResource() {
        this.client = HttpClient.newBuilder().build();
        this.micaUri = URI.create(requireNonNull(System.getenv("MICA_URI")));
        this.micaApiKey = requireNonNull(System.getenv("MICA_API_KEY"));
    }

    @POST
    @Path("render")
    @Consumes(APPLICATION_JSON)
    public Response render(InputStream in) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(micaUri.resolve("/api/mica/v1/view/render"))
                .POST(BodyPublishers.ofInputStream(() -> in))
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .header(AUTHORIZATION, micaApiKey)
                .build();

        var response = client.send(request, BodyHandlers.ofInputStream());
        return Response.status(response.statusCode())
                .header(CONTENT_TYPE, response.headers().firstValue(CONTENT_TYPE).orElse(APPLICATION_JSON))
                .entity((StreamingOutput) output -> IO.copy(response.body(), output))
                .build();
    }
}
