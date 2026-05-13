package ca.vanzyl.ck8s.cloudformation;

import java.util.Objects;

public class Resource {

    public static Resource plain(String value) {
        return new Resource(Type.PLAIN, value);
    }

    public static Resource sub(String value) {
        return new Resource(Type.SUB, value);
    }

    public enum Type {
        PLAIN, SUB
    }

    private final Type type;
    private final String value;

    protected Resource(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    public String value() {
        return value;
    }

    public Type type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Resource resource = (Resource) o;
        return type == resource.type && Objects.equals(value, resource.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        return "Resource{" +
                "type=" + type +
                ", value='" + value + '\'' +
                '}';
    }
}
