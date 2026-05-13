package ca.vanzyl.ck8s.state;

import ca.vanzyl.ck8s.common.Mapper;

import java.io.OutputStream;
import java.util.Map;

public class MapEntity implements Entity{

    private final String name;
    private final Map<String, Object> entity;

    public MapEntity(String name, Map<String, Object> entity) {
        this.name = name;
        this.entity = entity;
    }

    @Override
    public String entityName() {
        return name;
    }

    public Map<String, Object> entity() {
        return entity;
    }

    @Override
    public void dump(OutputStream out) {
        Mapper.yaml().write(out, entity);
    }
}
