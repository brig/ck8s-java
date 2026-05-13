package ca.vanzyl.concord.k8s;

import ca.vanzyl.ck8s.common.MapUtils;
import ca.vanzyl.ck8s.common.Mapper;
import com.walmartlabs.concord.sdk.Constants;
import com.walmartlabs.concord.server.process.Payload;
import com.walmartlabs.concord.server.process.ProcessException;
import com.walmartlabs.concord.server.process.pipelines.processors.ConfigurationProcessor;
import com.walmartlabs.concord.server.sdk.process.CustomEnqueueProcessor;
import dev.ybrig.ck8s.cli.common.Ck8sConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.Map;

import static ca.vanzyl.concord.k8s.PayloadUtils.mergeArg;

public class Ck8sRequestAttachmentProcessor implements CustomEnqueueProcessor {

    private static final Logger log = LoggerFactory.getLogger(Ck8sRequestAttachmentProcessor.class);

    @Override
    public Payload handleAttachments(Payload payload) {
        var p = payload.getAttachment(ConfigurationProcessor.REQUEST_ATTACHMENT_KEY);
        if (p == null) {
            return payload;
        }

        Map<String, Object> providedArgs;
        try {
            providedArgs = MapUtils.getMap(Mapper.json().readMap(p), Constants.Request.ARGUMENTS_KEY, Map.of());
        } catch (Exception e) {
            throw new ProcessException(payload.getProcessKey(), "Invalid request data format", e, Response.Status.BAD_REQUEST);
        }

        log.info("handleAttachments: providedArgs: {}", providedArgs);

        return mergeArg(payload, Ck8sConstants.Arguments.INPUT_ARGS, providedArgs);
    }
}
