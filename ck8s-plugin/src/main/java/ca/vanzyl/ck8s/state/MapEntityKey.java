package ca.vanzyl.ck8s.state;

public record MapEntityKey(String id, String type) implements EntityKey<MapEntity> {

    @Override
    public Class<MapEntity> entityType() {
        return MapEntity.class;
    }
}
