package ca.vanzyl.ck8s.k8s.actions;

import ca.vanzyl.ck8s.common.Mapper;
import ca.vanzyl.ck8s.k8s.K8sClientFactory;
import ca.vanzyl.ck8s.k8s.K8sTaskAction;
import ca.vanzyl.ck8s.k8s.K8sTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

public class ListEventsAction implements K8sTaskAction<K8sTaskParams.ListEventsParams> {

    private static final Logger log = LoggerFactory.getLogger(ListEventsAction.class);
    private static final Logger processLog = LoggerFactory.getLogger("processLog");

    @Override
    public Action action() {
        return Action.LIST_EVENTS;
    }

    @Override
    public TaskResult execute(Context context, K8sTaskParams.ListEventsParams input) throws Exception {
        var namespace = input.namespace();
        var dumpEvents = input.dumpEvents();

        try (var client = K8sClientFactory.create(input)) {
            var events = client.v1().events().inNamespace(namespace).list().getItems();
            events.sort(Comparator.comparing(e -> e.getMetadata().getCreationTimestamp()));

            log.info("Found {} events in '{}' namespace", events.size(), namespace);

            if (dumpEvents) {
                processLog.info("{}", String.format("%-10s %-9s %-25s %-45s %s",
                        "LAST SEEN", "TYPE", "REASON", "OBJECT", "MESSAGE"));

                for (var e : events) {
                    var object = e.getInvolvedObject().getKind() + "/" + e.getInvolvedObject().getName();

                    processLog.info("{}", String.format("%-10s %-9s %-25s %-45s %s",
                            getLastSeen(e), e.getType(), e.getReason(), object,
                            colorForEvent(e.getType(), e.getReason(), e.getMessage())));
                }
                return TaskResult.success();
            } else {
                return TaskResult.success()
                        .value("events", events.stream().map(p -> Mapper.json().convertToMap(p)).toList());
            }
        } catch (KubernetesClientException e) {
            log.error("❌ Failed to list events in namespace '{}': {}", namespace, e.getMessage());
            return TaskResult.fail(e);
        }
    }

    private static String getLastSeen(io.fabric8.kubernetes.api.model.Event e) {
        var ts = e.getLastTimestamp();
        if (ts == null) {
            ts = e.getEventTime() != null ? e.getEventTime().toString() : e.getFirstTimestamp();
        }
        if (ts == null) {
            return "-";
        }

        try {
            var eventTime = OffsetDateTime.parse(ts, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            var diff = Duration.between(eventTime, OffsetDateTime.now());
            if (diff.toMinutes() < 1) {
                return diff.getSeconds() + "s";
            } else if (diff.toHours() < 1) {
                return diff.toMinutes() + "m";
            } else if (diff.toDays() < 1) {
                return diff.toHours() + "h";
            } else {
                return diff.toDays() + "d";
            }
        } catch (Exception ex) {
            return ts;
        }
    }

    private static String colorForEvent(String type, String reason, String text) {
        final String RESET = "\u001B[0m";
        final String RED = "\u001B[31m";
        final String YELLOW = "\u001B[33m";

        if (type == null) {
            if (reason != null && reason.startsWith("Failed")) {
                return RED + text + RESET;
            } else {
                return text;
            }
        }

        return switch (type) {
            case "Normal" -> text;
            case "Warning" -> RED + text + RESET;
            default -> YELLOW + text + RESET;
        };
    }
}
