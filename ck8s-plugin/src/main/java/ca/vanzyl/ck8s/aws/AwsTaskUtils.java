package ca.vanzyl.ck8s.aws;

import ca.vanzyl.ck8s.common.MapUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AwsTaskUtils {

    private static final ObjectMapper serializeMapper = createSerializeMapper();
    private static final ObjectMapper deserializeMapper = createDeserializeMapper();

    public static List<Map<String, Object>> serializeList(List<? extends ToCopyableBuilder<?, ?>> o) {
        if (o == null) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (ToCopyableBuilder<?, ?> item : o) {
            result.add(serialize(item));
        }
        return result;
    }

    public static Map<String, Object> serialize(ToCopyableBuilder<?, ?> o) {
        if (o == null) {
            return null;
        }

        try {
            String str = serializeMapper.writeValueAsString(o.toBuilder());
            return serializeMapper.readValue(str, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T deserialize(Map<String, Object> o, Class<T> clazz) {
        return deserializeMapper.convertValue(o, clazz);
    }

    public static <T> T deserialize(Path p, Class<T> clazz) {
        try {
            return deserializeMapper.readValue(p.toFile(), clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> deserializeList(List<Map<String, Object>> attributes, Class<T> clazz) {
        try {
            var type = deserializeMapper.getTypeFactory().constructCollectionType(List.class, clazz);
            return deserializeMapper.convertValue(attributes, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> deserializeList(Path p, Class<T> clazz) {
        try {
            var type = deserializeMapper.getTypeFactory().constructCollectionType(List.class, clazz);
            return deserializeMapper.readValue(p.toFile(), type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Region assertRegion(Variables input) {
        String region = input.assertString("region");
        return Region.of(region);
    }

    public static Region assertRegion(Context context, Variables input) {
        var region = input.getString("region");
        if (region == null) {
            region = MapUtils.assertString(clusterRequest(context), "region");
        }
        return Region.of(region);
    }

    public static String getProfile(Variables input) {
        return input.getString("profile");
    }

    public static String getProfile(Context context) {
        var envars = MapUtils.getMap(clusterRequest(context), "envars", Map.of());
        return MapUtils.getString(envars, "AWS_PROFILE");
    }

    public static String getProfile(Context context, Variables input) {
        if (input.has("profile")) {
            return getProfile(input);
        }

        return getProfile(context);
    }

    private static ObjectMapper createDeserializeMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        om.setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);
        return om;
    }

    private static ObjectMapper createSerializeMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return om;
    }

    private static Map<String, Object> clusterRequest(Context context) {
        return context.variables().assertMap("clusterRequest");
    }

    private AwsTaskUtils() {
    }
}
