package ca.vanzyl.ck8s.preview;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.walmartlabs.concord.runtime.v2.runner.PersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Singleton
public class DefaultPreviewChangesRecorder implements PreviewChangesRecorder {

    private static final Logger log = LoggerFactory.getLogger(DefaultPreviewChangesRecorder.class);

    private static final TypeReference<List<Change>> STEPS_TYPE = new TypeReference<>() {
    };

    private static final String FILE_NAME = "dry-run-preview.yaml";

    private final PersistenceService persistenceService;
    private final ObjectMapper objectMapper;

    @Inject
    public DefaultPreviewChangesRecorder(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
        this.objectMapper = new ObjectMapper(
                new YAMLFactory()
                        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module());
    }

    @Override
    public synchronized void record(Change change) {
        persistenceService.persistFile(FILE_NAME,
                out -> objectMapper.writeValue(out, List.of(change)),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @Override
    public List<Change> list() {
        var result = persistenceService.loadPersistedFile(FILE_NAME, in -> objectMapper.readValue(in, STEPS_TYPE));
        if (result == null) {
            return List.of();
        }
        return result;
    }

    @Override
    public synchronized void cleanup() {
        try {
            persistenceService.deletePersistedFile(FILE_NAME);
        } catch (IOException e) {
            log.warn("Can't cleanup records from state: {}", e.getMessage());
        }
    }
}
