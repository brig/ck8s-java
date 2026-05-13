package ca.vanzyl.ck8s.actions;

import java.util.EnumSet;
import java.util.Set;

public interface ActionName {

    String value();

    static <T extends Enum<T> & ActionName> T fromValue(Class<T> enumClass, String value) {
        for (T action : enumClass.getEnumConstants()) {
            if (action.value().equals(value)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown action: '" + value + "'. Available actions: " + knownValues(enumClass));
    }

    static <T extends Enum<T>> Set<T> knownValues(Class<T> enumClass) {
        return EnumSet.allOf(enumClass);
    }
}
