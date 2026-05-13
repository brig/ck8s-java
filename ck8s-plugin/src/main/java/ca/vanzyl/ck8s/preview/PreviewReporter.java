package ca.vanzyl.ck8s.preview;

import ca.vanzyl.ck8s.actions.DryRunPhases;
import ca.vanzyl.ck8s.common.MapUtils;
import ca.vanzyl.ck8s.state.StateChangesProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.runtime.v2.runner.PersistenceService;
import com.walmartlabs.concord.runtime.v2.sdk.ProcessConfiguration;
import com.walmartlabs.concord.svm.*;
import com.walmartlabs.concord.svm.Runtime;
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

@Priority(10)
public class PreviewReporter implements ExecutionListener {

    public static final Logger log = LoggerFactory.getLogger(PreviewReporter.class);

    private static final String PROCESS_INFO_FILENAME = "process-info.json";
    private static final String REPORT_FILENAME = "preview.txt";

    private final ObjectMapper objectMapper;
    private final PersistenceService persistenceService;
    private final PreviewChangesRecorder recorder;
    private ProcessInfo processInfo;

    // TODO: remove me after priority support in Concord
    private final StateChangesProducer stateChangesProducer;

    @Inject
    public PreviewReporter(ObjectMapper objectMapper, PersistenceService persistenceService, PreviewChangesRecorder recorder, StateChangesProducer stateChangesProducer) {
        this.objectMapper = objectMapper;
        this.persistenceService = persistenceService;
        this.recorder = recorder;
        this.stateChangesProducer = stateChangesProducer;
    }

    @Override
    public void beforeProcessStart(Runtime runtime, State state) {
        var processConfiguration = runtime.getService(ProcessConfiguration.class);

        if (!DryRunPhases.isPreview(processConfiguration)) {
            return;
        }

        // cleanup phase argument
        var args = new HashMap<>(MapUtils.getMap(processConfiguration.arguments(), "inputArgs", Map.of()));
        args.remove("phase");

        this.processInfo = ProcessInfo.builder()
                .cluster(MapUtils.getString(processConfiguration.arguments(), "clientCluster", "n/a"))
                .flow(MapUtils.getString(processConfiguration.arguments(), "flow", "n/a"))
                .args(args)
                .build();
    }

    @Override
    public void beforeProcessResume(Runtime runtime, State state) {
        this.processInfo = persistenceService.loadPersistedFile(PROCESS_INFO_FILENAME,
                is -> objectMapper.readValue(is, ProcessInfo.class));
    }

    @Override
    public void onProcessError(com.walmartlabs.concord.svm.Runtime runtime, State state, Exception e) {
        if (isDisabled()) {
            return;
        }

        this.stateChangesProducer.onProcessError(runtime, state, e);

        generateReport();
    }

    @Override
    public void afterProcessEnds(Runtime runtime, State state, Frame lastFrame) {
        if (isDisabled()) {
            return;
        }

        if (isSuspended(state)) {
            persistenceService.persistFile(PROCESS_INFO_FILENAME,
                    out -> objectMapper.writeValue(out, this.processInfo));

            return;
        }

        this.stateChangesProducer.afterProcessEnds(runtime, state, lastFrame);

        generateReport();
    }

    private boolean isDisabled() {
        return this.processInfo == null;
    }

    private void generateReport() {
        log.info("Generating preview report...");

        try {
            var reportWriter = new PlainTextReportWriter(processInfo, recorder.list());

//            recorder.cleanup();

            persistenceService.persistFile(REPORT_FILENAME, reportWriter::write, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            persistenceService.deletePersistedFile(PROCESS_INFO_FILENAME);
        } catch (Exception e) {
            throw new RuntimeException("Can't generate preview report: " + e.getMessage(), e);
        }

        log.info("Preview info saved as attachment with name '{}'", REPORT_FILENAME);

        var report = persistenceService.loadPersistedFile(REPORT_FILENAME, is -> new String(is.readAllBytes(), StandardCharsets.UTF_8));

        log.info("\n\n{}", report);
    }

    private static boolean isSuspended(State state) {
        return state.threadStatus().entrySet().stream()
                .anyMatch(e -> e.getValue() == ThreadStatus.SUSPENDED);
    }
}
