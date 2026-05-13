package ca.vanzyl.ck8s.state;

import ca.vanzyl.ck8s.asserts.json.JsonComparator;
import ca.vanzyl.ck8s.asserts.json.JsonCompareResult;
import ca.vanzyl.ck8s.preview.Change;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

public class MapEntityChangesProducer implements EntityChangeProducer<MapEntityKey, MapEntity> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Class<MapEntity> entityType() {
        return MapEntity.class;
    }

    @Override
    public List<Change> produce(MapEntityKey key, MapEntity prev, MapEntity current, Instant lastModified) {
        if (prev == null && current == null) {
            return List.of();
        }

        if (current == null) {
            return List.of(Change.delete(key.id())
                    .type(key.type())
                    .metadata(Change.Metadata.builder().name(prev.entityName()).build())
                    .timestamp(lastModified)
                    .build());
        }

        if (prev == null) {
            return List.of(Change.create(key.id())
                    .type(key.type())
                    .metadata(Change.Metadata.builder().name(current.entityName()).build())
                    .timestamp(lastModified)
                    .build());
        }

        var oneNode = objectMapper.convertValue(prev.entity(), JsonNode.class);
        var twoNode = objectMapper.convertValue(current.entity(), JsonNode.class);

        JsonCompareResult result = new JsonComparator(true)
                .compare(oneNode, twoNode);

        if (result.success()) {
            return List.of();
        }

        return List.of(Change.update(key.id())
                .type(key.type())
                .diffMessage(result.message())
                .metadata(Change.Metadata.builder().name(current.entityName()).build())
                .timestamp(lastModified)
                .build());
    }
}
