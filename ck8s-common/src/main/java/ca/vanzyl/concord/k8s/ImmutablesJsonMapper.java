package ca.vanzyl.concord.k8s;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class ImmutablesJsonMapper
{

    private final ObjectMapper mapper;

    public ImmutablesJsonMapper()
    {
        mapper = new ObjectMapper();
        mapper.registerModule(new GuavaModule());
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public <T> T read(File input, Class<T> clazz)
            throws IOException
    {
        return mapper.readValue(input, clazz);
    }

    public <T> T read(InputStream input, Class<T> clazz)
            throws IOException
    {
        return mapper.readValue(input, clazz);
    }

    public <T> T read(String input, Class<T> clazz)
            throws IOException
    {
        return mapper.readValue(input, clazz);
    }

    public <T> T read(byte[] input, Class<T> clazz)
            throws IOException
    {
        return mapper.readValue(input, clazz);
    }

    public <T> T convert(Map<String, Object> input, Class<T> clazz)
            throws IOException
    {
        return mapper.convertValue(input, clazz);
    }

    public <T> String write(T instance)
            throws IOException
    {
        return mapper.writeValueAsString(instance);
    }

    public <T> void write(OutputStream out, T instance)
            throws IOException
    {
        mapper.writeValue(out, instance);
    }
}
