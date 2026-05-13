package ca.vanzyl.ck8s.secrets.gcp;

import ca.vanzyl.ck8s.common.MapUtils;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.Collections;
import java.util.Map;

@Named("gsmSecrets")
public class GsmSecretsTaskV2
        implements Task
{

    private static final Logger log = LoggerFactory.getLogger(GsmSecretsTaskV2.class);

    @Override
    public TaskResult execute(Variables input)
            throws Exception
    {
        log.debug("Debugging secrets from {} ", input.toMap().toString());
        Map<String, Object> clusterRequest = input.getMap("clusterRequest", Collections.emptyMap());
        String secretsDocument = MapUtils.getString(clusterRequest, "gcp.secretsDocument");
        if (secretsDocument == null) {
            secretsDocument = input.getString("secretsDocument");
        }
        if (secretsDocument == null) {
            log.info("secretsDocument is null");
            return TaskResult.success();
        }
        log.info("Fetching secrets from {} ", secretsDocument);
        GsmSecretsRetriever secretsPopulator = new GsmSecretsRetriever(secretsDocument);
        Map<String, String> secrets = secretsPopulator.map();
        return TaskResult.success()
                .value("secrets", secrets);
    }
}
