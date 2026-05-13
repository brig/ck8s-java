package ca.vanzyl.ck8s.aws.rds.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.aws.rds.RdsClientFactory;
import ca.vanzyl.ck8s.aws.rds.RdsTaskAction;
import ca.vanzyl.ck8s.aws.rds.RdsTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.*;

import javax.inject.Inject;
import java.util.Set;

public class FetchEndpointAction extends RdsTaskAction<RdsTaskParams.FetchEndpointParams> {

    private final static Logger log = LoggerFactory.getLogger(FetchEndpointAction.class);

    @Inject
    public FetchEndpointAction(RdsClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.FETCH_ENDPOINT;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, RdsTaskParams.FetchEndpointParams input) throws Exception {
        var engine = input.engine();
        var identifier = input.identifier();

        try (var client = createClient(input)) {
            String endpoint;
            if (engine.toLowerCase().contains("aurora")) {
                endpoint = getAuroraEndpoint(client, identifier);
            } else {
                endpoint = getRdsEndpoint(client, identifier);
            }

            if (endpoint == null) {
                log.info("No endpoint found for identifier '{}'", identifier);
                return TaskResult.success();
            }
            log.info("✅ Resolved '{}'@'{}' endpoint '{}'", engine, identifier, endpoint);
            return TaskResult.success()
                    .value("endpoint", endpoint);
        } catch (RdsException e) {
            log.error("❌ Failed to retrieve or write endpoint: {}", e.getMessage(), e);
            return TaskResult.fail(e);
        }
    }

    private static String getRdsEndpoint(RdsClient client, String identifier) {
        try {
            var response = client.describeDBInstances(
                    DescribeDbInstancesRequest.builder()
                            .dbInstanceIdentifier(identifier)
                            .build());

            for (var instance : response.dbInstances()) {
                if (identifier.equals(instance.dbInstanceIdentifier())) {
                    return instance.endpoint().address();
                }
            }
            return null;
        } catch (DbInstanceNotFoundException e) {
            return null;
        }
    }

    private static String getAuroraEndpoint(RdsClient client, String identifier) {
        try {
            var clusters = client.describeDBClusters(
                            DescribeDbClustersRequest.builder()
                                    .dbClusterIdentifier(identifier)
                                    .build())
                    .dbClusters();

            if (clusters.size() > 1) {
                throw new RuntimeException("Multiple Aurora endpoints found for identifier '" + identifier + "'");
            } else if (clusters.isEmpty()) {
                return null;
            }

            return clusters.get(0).endpoint();
        } catch (DbClusterNotFoundException e) {
            return null;
        }
    }
}
