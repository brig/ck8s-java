package ca.vanzyl.ck8s.state;

@FunctionalInterface
public interface EntityLoader<K extends EntityKey<E>, E extends Entity> {

    E load(K key);
}
