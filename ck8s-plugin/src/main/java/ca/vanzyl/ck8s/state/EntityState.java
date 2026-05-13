package ca.vanzyl.ck8s.state;

import com.walmartlabs.concord.runtime.v2.runner.PersistenceService;
import com.walmartlabs.concord.svm.*;
import com.walmartlabs.concord.svm.Runtime;
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Singleton
@Priority(-2)
public class EntityState implements ExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(EntityState.class);

    private static final String STATE_FILENAME = "state.bin";

    private final HashMap<EntityKey<?>, Entity> initialState = new HashMap<>();
    private final HashMap<EntityKey<?>, Entity> currentState = new HashMap<>();
    private final HashMap<EntityKey<?>, Instant> lastModified = new HashMap<>();

    private final Lock lock = new ReentrantLock();

    private final PersistenceService persistenceService;

    @Inject
    public EntityState(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @Override
    public void beforeProcessResume(Runtime runtime, State state) {
        var stateRecord = persistenceService.load(STATE_FILENAME, StateRecord.class);
        if (stateRecord != null) {
            this.initialState.putAll(stateRecord.initialState());
            this.currentState.putAll(stateRecord.currentState());
            this.lastModified.putAll(stateRecord.lastModified());
        }

        log.info("State restored");
    }

    @Override
    public void afterProcessEnds(Runtime runtime, State state, Frame lastFrame) {
        if (isSuspended(state)) {
            try {
                if (!currentState.isEmpty()) {
                    persistenceService.save(STATE_FILENAME, new StateRecord(initialState, currentState, lastModified));
                }
            } catch (Exception e) {
                throw new RuntimeException("Can't save state: " + e.getMessage(), e);
            }
        }
    }

    public <E extends Entity> E get(EntityKey<E> key) {
        lock.lock();
        try {
            var e = currentState.get(key);
            return entityTypeSafeCast(e, key.entityType());
        } finally {
            lock.unlock();
        }
    }

    public <K extends EntityKey<E>, E extends Entity> E getOrLoad(K key, EntityLoader<K, E> loader) {
        lock.lock();
        try {
            if (currentState.containsKey(key)) {
                var e = currentState.get(key);
                return entityTypeSafeCast(e, key.entityType());
            }

            E entity = loader.load(key);
            put(key, entity);
            return entity;
        } finally {
            lock.unlock();
        }
    }

    public <E extends Entity> E getInitial(EntityKey<E> key) {
        lock.lock();
        try {
            var e = initialState.get(key);
            return entityTypeSafeCast(e, key.entityType());
        } finally {
            lock.unlock();
        }
    }

    public <E extends Entity> void put(EntityKey<E> key, E entity) {
        lock.lock();
        try {
            if (!initialState.containsKey(key)) {
                initialState.put(key, entity);
            }
            currentState.put(key, entity);
            lastModified.put(key, Instant.now());
        } finally {
            lock.unlock();
        }
    }

    public <E extends Entity> void delete(EntityKey<E> key) {
        lock.lock();
        try {
            currentState.put(key, null);
            lastModified.put(key, Instant.now());
        } finally {
            lock.unlock();
        }
    }

    public Instant getLastModified(EntityKey<?> key) {
        lock.lock();
        try {
            return lastModified.get(key);
        } finally {
            lock.unlock();
        }
    }

    public Set<? extends EntityKey<? extends Entity>> list() {
        lock.lock();
        try {
            return Collections.unmodifiableSet(currentState.keySet());
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public <K extends EntityKey<E>, E extends Entity> List<Map.Entry<K, E>> list(Class<E> entityType) {
        lock.lock();
        try {
            return currentState.entrySet().stream()
                    .filter(e -> e.getKey().entityType().equals(entityType))
                    .map(e -> new AbstractMap.SimpleEntry<>((K)e.getKey(), (E)e.getValue()))
                    .collect(Collectors.toList());
        } finally {
            lock.unlock();
        }
    }

    private <E extends Entity> E entityTypeSafeCast(Entity e, Class<E> expectedType) {
        if (e == null) {
            return null;
        }
        if (!expectedType.isInstance(e)) {
            throw new IllegalStateException("Type mismatch: expected " + expectedType + " but got " + e.getClass());
        }
        return expectedType.cast(e);
    }

    private static boolean isSuspended(com.walmartlabs.concord.svm.State state) {
        return state.threadStatus().entrySet().stream()
                .anyMatch(e -> e.getValue() == ThreadStatus.SUSPENDED);
    }

    private record StateRecord(HashMap<EntityKey<?>, Entity> initialState,
                               HashMap<EntityKey<?>, Entity> currentState,
                               HashMap<EntityKey<?>, Instant> lastModified) implements Serializable {

            @Serial
            private static final long serialVersionUID = 1L;

    }
}
