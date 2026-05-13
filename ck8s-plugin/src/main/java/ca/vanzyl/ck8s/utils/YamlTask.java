package ca.vanzyl.ck8s.utils;

import ca.vanzyl.ck8s.common.Mapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.DryRunReady;
import com.walmartlabs.concord.runtime.v2.sdk.Task;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Map;

@Named("ck8sYaml")
@DryRunReady
public class YamlTask
        implements Task
{

    private static final String RESOURCE_PREFIX = "resource_";
    private static final String YAML_FILE_SUFFIX = ".yaml";

    private final Path workDir;
    private final Context context;

    @Inject
    public YamlTask(Context context)
    {
        this.workDir = context.workingDirectory();
        this.context = context;
    }

    public Object fromString(String yaml) throws IOException {
        return Mapper.yaml().read(yaml, new TypeReference<>() {});
    }

    public String indentYaml(Map<String, String> map, int indent)
    {
        return context.eval(YamlUtils.indentYaml(map, indent), String.class);
    }

    public String indentArray(Object o, int indent) {
        if (o == null) {
            return "";
        }

        if (o instanceof Collection<?>) {
            Collection<?> arr = (Collection<?>) o;
            return YamlUtils.indentYaml(arr, indent);
        } else {
            String str = o.toString();
            if (str.trim().isEmpty()) {
                return "";
            }
            return indent("- \"" + str + "\"" , indent);
        }
    }

    public String nindentArray(Object properties, int indent) {
        return "\n" + indentArray(properties, indent);
    }

    public String nindentYaml(Map<String, String> map, int indent)
    {
        return context.eval(YamlUtils.nindentYaml(map, indent), String.class);
    }

    @Deprecated
    public String toYaml(Map<String, String> map, int indent)
    {
        return context.eval(YamlUtils.nindentYaml(map, indent), String.class);
    }

    public String toCsv(Map<String, String> map)
    {
        return context.eval(YamlUtils.toCsv(map), String.class);
    }

    public String indent(String text, int indent)
    {
        return YamlUtils.indent(text, indent);
    }

    public String nindent(String text, int indent)
    {
        return "\n" + YamlUtils.indent(text, indent);
    }

    // remove me after concord 2.7.x released
    public String writeAsYaml(Object content) throws IOException {
        Path tmpFile = createTempFile(RESOURCE_PREFIX, YAML_FILE_SUFFIX);
        writeToFile(tmpFile, p -> {
            try (OutputStream out = Files.newOutputStream(p)) {
                createObjectMapper(new YAMLFactory()
                        .disable(YAMLGenerator.Feature.SPLIT_LINES)
                        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
                        .writerWithDefaultPrettyPrinter()
                        .writeValue(out, content);
            }
        });
        return workDir.relativize(tmpFile.toAbsolutePath()).toString();
    }

    public String writeAsYaml(Object content, String path) throws IOException {
        Path dst = assertWorkDirPath(path);
        Files.createDirectories(dst.getParent());

        writeToFile(dst, p -> {
            try (OutputStream out = Files.newOutputStream(p, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                createObjectMapper(new YAMLFactory()
                        .disable(YAMLGenerator.Feature.SPLIT_LINES)
                        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
                        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                        .writerWithDefaultPrettyPrinter()
                        .writeValue(out, content);
            }
        });
        return workDir.relativize(dst.toAbsolutePath()).toString();
    }

    private Path createTempFile(String resourcePrefix, String yamlFileSuffix) throws IOException {
        return context.fileService().createTempFile(resourcePrefix, yamlFileSuffix);
    }

    private static ObjectMapper createObjectMapper(JsonFactory jf) {
        ObjectMapper om = new ObjectMapper(jf);
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        return om;
    }

    static void writeToFile(Path file, PathHandler h) throws IOException {
        h.handle(file);
    }

    private Path assertWorkDirPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        Path dst = Paths.get(path);
        if (!dst.isAbsolute()) {
            dst = workDir.resolve(path).normalize().toAbsolutePath();
        }
        if (!dst.startsWith(workDir)) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
        return dst;
    }

    private interface PathHandler {

        void handle(Path path) throws IOException;
    }
}
