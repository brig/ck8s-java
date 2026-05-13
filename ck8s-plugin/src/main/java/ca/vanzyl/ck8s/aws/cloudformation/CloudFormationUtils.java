package ca.vanzyl.ck8s.aws.cloudformation;

import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static software.amazon.awssdk.services.cloudformation.model.StackStatus.*;

public final class CloudFormationUtils {

    private static final Logger log = LoggerFactory.getLogger(CloudFormationUtils.class);

    private static final long POLL_INTERVAL = TimeUnit.SECONDS.toMillis(10);

    // see: DefaultCloudFormationWaiter.stackCreateCompleteWaiterAcceptors
    public static final List<StackStatus> STACK_CREATE_OK_STATUSES = List.of(CREATE_COMPLETE, UPDATE_COMPLETE, UPDATE_IN_PROGRESS, UPDATE_COMPLETE_CLEANUP_IN_PROGRESS, UPDATE_FAILED, UPDATE_ROLLBACK_IN_PROGRESS, UPDATE_ROLLBACK_FAILED, UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS, UPDATE_ROLLBACK_COMPLETE);
    public static final List<StackStatus> STACK_CREATE_ERROR_STATUSES = List.of(CREATE_FAILED, DELETE_COMPLETE, DELETE_FAILED, ROLLBACK_FAILED, ROLLBACK_COMPLETE);

    public static final List<StackStatus> STACK_UPDATE_OK_STATUSES = List.of(UPDATE_COMPLETE);
    public static final List<StackStatus> STACK_UPDATE_ERROR_STATUSES = List.of(UPDATE_FAILED, UPDATE_ROLLBACK_FAILED, UPDATE_ROLLBACK_COMPLETE);

    public static final List<StackStatus> STACK_IMPORT_OK_STATUSES = List.of(IMPORT_COMPLETE);
    public static final List<StackStatus> STACK_IMPORT_ERROR_STATUSES = List.of(ROLLBACK_COMPLETE, ROLLBACK_FAILED, IMPORT_ROLLBACK_IN_PROGRESS, IMPORT_ROLLBACK_FAILED, IMPORT_ROLLBACK_COMPLETE);

    public static final List<StackStatus> STACK_DELETE_OK_STATUSES = List.of(DELETE_COMPLETE);
    public static final List<StackStatus> STACK_DELETE_ERROR_STATUSES = List.of(DELETE_FAILED, CREATE_FAILED, ROLLBACK_FAILED, UPDATE_ROLLBACK_IN_PROGRESS, UPDATE_ROLLBACK_FAILED, UPDATE_ROLLBACK_COMPLETE, UPDATE_COMPLETE);

    public static final List<ChangeSetStatus> CHANGE_SET_CREATE_OK_STATUSES = List.of(ChangeSetStatus.CREATE_COMPLETE);
    public static final List<ChangeSetStatus> CHANGE_SET_CREATE_ERROR_STATUSES = List.of(ChangeSetStatus.FAILED);


    public static boolean stackExists(CloudFormationClient client, String stackName) {
        try {
            client.describeStacks(DescribeStacksRequest.builder()
                    .stackName(stackName)
                    .build());
            return true;
        } catch (AwsServiceException e) {
            if ("ValidationError".equals(e.awsErrorDetails().errorCode())) {
                return false;
            }
            throw e;
        }
    }

    public static void waitForStackCreated(CloudFormationClient client, String stackName) {
        waitForStackCompletion(client, stackName, STACK_CREATE_OK_STATUSES, STACK_CREATE_ERROR_STATUSES, "create");
    }

    public static void waitForStackDelete(CloudFormationClient client, String stackName) {
        waitForStackCompletion(client, stackName, STACK_DELETE_OK_STATUSES, STACK_DELETE_ERROR_STATUSES, "delete");
    }

    public static void waitStackUpdated(CloudFormationClient client, String stackName) {
        waitForStackCompletion(client, stackName, STACK_UPDATE_OK_STATUSES, STACK_UPDATE_ERROR_STATUSES, "update");
    }

    public static void waitForChangeSetCreated(CloudFormationClient client, String stackName, String changeSetName) {
        waitForChangeSetCompletion(client, stackName, changeSetName, CHANGE_SET_CREATE_OK_STATUSES, CHANGE_SET_CREATE_ERROR_STATUSES, "create");
    }

    public static void waitForStackCompletion(CloudFormationClient client, String stackName,
                                               List<StackStatus> okStatuses, List<StackStatus> errorStatuses,
                                               String action) throws CloudFormationException {

        log.info("Waiting for stack '{}' to reach a final state...", stackName);

        while (!Thread.currentThread().isInterrupted()) {
            var status = getStackStatus(client, stackName, action);

            if (status != null) {
                log.info("Current stack status: {}", status);

                if (okStatuses.contains(status)) {
                    log.info("✅ Stack '{}' {} complete", stackName, action);
                    return;
                } else if (errorStatuses.contains(status)) {
                    log.info("❌ Stack '{}' {} failed: '{}'", stackName, action, status);

                    dumpStackEvents(client, stackName);

                    throw new UserDefinedException(String.format("Stack '%s' %s failed", stackName, action));
                }
            }

            sleep(POLL_INTERVAL);
        }
    }

    public static StackStatus getStackStatus(CloudFormationClient client, String stackName, String action) {
        Stack stack;
        if ("delete".equals(action)) {
            try {
                stack = describeStack(client, stackName);
            } catch (AwsServiceException e) {
                return STACK_DELETE_OK_STATUSES.get(0);
            }
        } else {
            stack = describeStack(client, stackName);
        }

        if (stack == null) {
            return null;
        }

        return stack.stackStatus();
    }

    public static Stack describeStack(CloudFormationClient client, String stackName) {
        List<Stack> stacks;
        try {
            stacks = client.describeStacks(DescribeStacksRequest.builder().stackName(stackName).build()).stacks();
        } catch (AwsServiceException e) {
            if ("ValidationError".equals(e.awsErrorDetails().errorCode())) {
                throw e;
            }

            log.warn("Failed to describe stack '{}': {}", stackName, e.awsErrorDetails().errorMessage());

            return null;
        } catch (Exception e) {
            log.warn("Failed to describe stack '{}': {}", stackName, e.getMessage());
            return null;
        }

        if (stacks.isEmpty()) {
            throw new UserDefinedException(String.format("No stacks with '%s' name", stackName));
        } else if (stacks.size() > 1) {
            throw new UserDefinedException(String.format("More than one with '%s' name: %s", stackName, stacks));
        }

        return stacks.get(0);
    }

    public static void dumpStackEvents(CloudFormationClient client, String stackName) {
        var eventsResponse = client.describeStackEvents(DescribeStackEventsRequest.builder()
                .stackName(stackName)
                .build());

        log.info("\uD83D\uDCDD Latest (5) stack events:");
        eventsResponse.stackEvents().stream()
                .limit(5)
                .forEach(event -> log.info("{} - {} - {}",
                        event.timestamp(), event.resourceStatusAsString(), orEmpty(event.resourceStatusReason())));
    }

    private static void waitForChangeSetCompletion(CloudFormationClient client, String stackName,
                                                   String changeSetName,
                                                   List<ChangeSetStatus> okStatuses, List<ChangeSetStatus> errorStatuses,
                                                   String action) throws CloudFormationException {
        log.info("Waiting for change set '{}/{}' to reach a final state...", stackName, changeSetName);

        while (!Thread.currentThread().isInterrupted()) {
            var changeSet = describeChangeSet(client, stackName, changeSetName);

            if (changeSet != null) {
                var status = changeSet.status();
                log.info("Current change set status: {}", status);

                if (okStatuses.contains(status)) {
                    log.info("✅ Change set '{}' {} complete", changeSetName, action);
                    return;
                } else if (errorStatuses.contains(status)) {
                    log.info("❌ Change set '{}' {} failed: '{}'. Reason: {}", changeSetName, action, status, changeSet.statusReason());

                    throw new UserDefinedException(String.format("Change set '%s/%s' %s failed", stackName, changeSetName, action));
                }
            }

            sleep(POLL_INTERVAL);
        }
    }

    private static DescribeChangeSetResponse describeChangeSet(CloudFormationClient client, String stackName, String changeSetName) {
        try {
            return client.describeChangeSet(DescribeChangeSetRequest.builder()
                    .stackName(stackName)
                    .changeSetName(changeSetName)
                    .build());
        } catch (AwsServiceException e) {
            if ("ValidationError".equals(e.awsErrorDetails().errorCode())) {
                throw e;
            }

            log.warn("Failed to describe change set '{}': {}", stackName, e.awsErrorDetails().errorMessage());

            return null;
        } catch (Exception e) {
            log.warn("Failed to describe change set '{}': {}", stackName, e.getMessage());
            return null;
        }
    }

    public static TaskResult handleNoUpdates(String errorPrefix, CloudFormationException e) {
        if (e.awsErrorDetails().errorMessage().contains("No updates are to be performed")) {
            log.info("No changes detected. Stack is already up to date.");
            return TaskResult.success();
        } else {
            log.error("❌ {}{}", errorPrefix, e.awsErrorDetails().errorMessage());
            return TaskResult.fail(e);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String orEmpty(String s) {
        if (s != null) {
            return s;
        }
        return "";
    }

    private CloudFormationUtils() {
    }
}
