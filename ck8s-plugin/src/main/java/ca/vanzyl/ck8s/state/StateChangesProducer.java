package ca.vanzyl.ck8s.state;

import ca.vanzyl.ck8s.preview.Change;
import ca.vanzyl.ck8s.preview.PreviewChangesRecorder;
import com.walmartlabs.concord.runtime.v2.runner.PersistenceService;
import com.walmartlabs.concord.svm.*;
import com.walmartlabs.concord.svm.Runtime;
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.PrintWriter;
import java.util.*;

@Priority(-100)
public class StateChangesProducer implements ExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(StateChangesProducer.class);

    private final EntityState entityState;
    private final PreviewChangesRecorder changesRecorder;
    private final PersistenceService persistenceService;

    private final Map<Class<Entity>, EntityChangeProducer<EntityKey<Entity>, Entity>> changeProducers;

    @Inject
    public StateChangesProducer(EntityState entityState,
                                PreviewChangesRecorder changesRecorder,
                                PersistenceService persistenceService,
                                Collection<EntityChangeProducer<?, ?>> changeProducers) {
        this.entityState = entityState;
        this.changesRecorder = changesRecorder;
        this.persistenceService = persistenceService;
        this.changeProducers = toMap(changeProducers);
    }

    @SuppressWarnings("unchecked")
    private Map<Class<Entity>, EntityChangeProducer<EntityKey<Entity>, Entity>> toMap(Collection<EntityChangeProducer<?, ?>> changeProducers) {
        var map = new HashMap<Class<Entity>, EntityChangeProducer<EntityKey<Entity>, Entity>>();
        for (var p : changeProducers) {
            var prev = map.put((Class<Entity>) p.entityType(), (EntityChangeProducer<EntityKey<Entity>, Entity>) p);
            if (prev != null) {
                log.warn("Duplicate change producer for {}", p.entityType());
            }
        }
        return map;
    }

    @Override
    public void afterProcessEnds(Runtime runtime, State state, Frame lastFrame) {
        if (isSuspended(state)) {
            return;
        }

        generateChanges();
    }

    @Override
    public void onProcessError(Runtime runtime, State state, Exception e) {
        generateChanges();
    }

    @SuppressWarnings("unchecked")
    private void generateChanges() {
        var changes = new ArrayList<Change>();
        var currentKeys = entityState.list();
        if (currentKeys.isEmpty()) {
            return;
        }

        log.info("Generating state changes ({} entities)...", currentKeys.size());

        for (var k : currentKeys) {
            var initialEntity = entityState.getInitial(k);
            var currentEntity = entityState.get(k);
            var lastModified = entityState.getLastModified(k);

            List<Change> entityChanges = List.of();
            var producer = changeProducers.get(k.entityType());
            if (producer == null) {
                log.warn("Undefined changes producer for entity type: {}", k.entityType());
            } else {
                entityChanges = producer.produce((EntityKey<Entity>) k, initialEntity, currentEntity, lastModified);
            }

            if (initialEntity != null) {
                persistenceService.persistFile(initialEntity.entityName() + ".prev.txt", initialEntity::dump);
            }
            if (currentEntity != null) {
                persistenceService.persistFile(currentEntity.entityName() + ".txt", currentEntity::dump);
            }

            var entityName = initialEntity != null ? initialEntity.entityName() : currentEntity != null ? currentEntity.entityName() : null;
            if (entityName != null) {
                for (int i = 0; i < entityChanges.size(); i++) {
                    var change = entityChanges.get(i);
                    if (change.diffMessage() != null) {
                        persistenceService.persistFile(entityName + "-" + i + ".diff.txt", out -> {
                            var writer = new PrintWriter(out);
                            writer.print(change.diffMessage());
                            writer.flush();
                        });
                    }
                }
            }
            changes.addAll(entityChanges);
        }

        log.info("Recorded: {}", changes);

        log.info("Recorded ({} changes)...", changes.size());

        changesRecorder.record(changes);
    }

    private static boolean isSuspended(com.walmartlabs.concord.svm.State state) {
        return state.threadStatus().entrySet().stream()
                .anyMatch(e -> e.getValue() == ThreadStatus.SUSPENDED);
    }
}
