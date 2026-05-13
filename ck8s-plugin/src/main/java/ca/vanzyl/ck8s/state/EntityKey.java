package ca.vanzyl.ck8s.state;

import java.io.Serializable;

public interface EntityKey<E extends Entity> extends Serializable {

    Class<E> entityType();
}
