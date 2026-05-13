package ca.vanzyl.concord.k8s;

import com.walmartlabs.concord.server.process.*;
import com.walmartlabs.concord.server.sdk.ConcordApplicationException;
import com.walmartlabs.concord.server.sdk.metrics.WithTimer;
import com.walmartlabs.concord.server.sdk.rest.Resource;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@javax.ws.rs.Path("/api/ck8s/v3/process")
@Tag(name = "Ck8s")
public class Ck8sProcessResourceV3 implements Resource {

    private static final Logger log = LoggerFactory.getLogger(Ck8sProcessResourceV3.class);

    private final ProcessManager processManager;
    private final PayloadManager payloadManager;

    @Inject
    public Ck8sProcessResourceV3(ProcessManager processManager,
                                 PayloadManager payloadManager) {
        this.processManager = processManager;
        this.payloadManager = payloadManager;
    }

    @POST
    @javax.ws.rs.Path("/{path: .*}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @WithTimer
    public StartProcessResponse start(MultipartInput input,
                                      @Context HttpServletRequest servletRequest) {

        try {
            Payload payload;
            try {
                payload = payloadManager.createPayload(input, servletRequest);
            } catch (IOException e) {
                log.error("start -> error creating a payload: {}", e.getMessage());
                throw new ConcordApplicationException("Error creating a payload", e);
            }

            return new StartProcessResponse(processManager.start(payload).getInstanceId());
        } finally {
            try {
                input.close();
            } catch (Exception e) {
                log.warn("close -> multipart close error: {}", e.getMessage());
            }
        }
    }
}
