package ca.vanzyl.ck8s.state;

import ca.vanzyl.ck8s.preview.Change;

import java.time.Instant;
import java.util.List;

public interface EntityChangeProducer<K extends EntityKey<E>, E extends Entity> {

    Class<E> entityType();

    List<Change> produce(K key, E prev, E current, Instant lastModified);
}
