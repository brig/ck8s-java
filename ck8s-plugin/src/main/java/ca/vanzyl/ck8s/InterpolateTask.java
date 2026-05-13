package ca.vanzyl.ck8s;

import ca.vanzyl.ck8s.utils.YamlValuesCleaner;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.walmartlabs.concord.runtime.v2.runner.context.ContextVariables;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static ca.vanzyl.ck8s.ExpressionExtractor.EXPR_PREFIX;

@Named("interpolate")
@DryRunReady
public class InterpolateTask
        implements Task
{

    private final static Logger log = LoggerFactory.getLogger(InterpolateTask.class);

    private final Context context;
    private final SensitiveDataHolder sensitiveDataHolder;

    @Inject
    public InterpolateTask(Context context, SensitiveDataHolder sensitiveDataHolder)
    {
        this.context = context;
        this.sensitiveDataHolder = sensitiveDataHolder;
    }

    @Override
    public TaskResult execute(Variables input)
            throws Exception
    {
        var file = new File(input.assertString("file"));
        if (!file.exists()) {
            log.warn("file '{}' does not exists", file);
            // TODO: throw exception?
            return TaskResult.success();
        }

        var debug = input.getBoolean("debug", context.processConfiguration().debug());

        // We need to take the file that is provided and interpolate the content with the
        // Concord context. This allows passing in just-in-time configuration values derived from
        // any Concord operations.
        //
        if (Files.size(file.toPath()) == 0) {
            log.warn("file '{}' is empty", file);
            return TaskResult.success();
        }

        var fileContent = new String(Files.readAllBytes(file.toPath()));
        Map<String, Object> additionalArgs = input.getMap("args", Collections.emptyMap());
        if (debug) {
            log.info("additional args:\n{}", additionalArgs);
            log.info("file content before:\n{}", fileContent);
        }

        //
        // We have interpolation work to do so we will backup the original file to another location
        // and then created a new interpolated version of the values.yaml in the original location.
        //
        var fileOriginal = new File(file + ".original");
        Files.copy(file.toPath(), fileOriginal.toPath(), StandardCopyOption.REPLACE_EXISTING);

        var interpolator = findInterpolator(input);
        var strictMode = input.getBoolean("strict", false);
        if (strictMode) {
            interpolator = new ConcordExpressionInterpolator(interpolator);
        }

        var maskSensitiveData = input.getBoolean("maskSensitiveData", false);
        var interpolatedFileContent = interpolator.eval(fileContent, additionalArgs, maskSensitiveData);
        Files.write(file.toPath(), interpolatedFileContent.getBytes());
        Files.deleteIfExists(fileOriginal.toPath());

        if (debug) {
            log.info("file content after:\n{}", interpolatedFileContent);
        }

        return TaskResult.success();
    }

    private Interpolator findInterpolator(Variables input) {
        var recursively = input.getBoolean("recursively", false);
        var removeEmptyValues = input.getBoolean("removeEmptyValues", false);
        var fileFormat = input.getString("fileFormat", "text");
        if ("yaml".equals(fileFormat)) {
            return new YamlInterpolator(context, sensitiveDataHolder, recursively, removeEmptyValues);
        } else if ("json".equals(fileFormat)) {
            return new JsonInterpolator(context, sensitiveDataHolder, recursively, removeEmptyValues);
        }
        return new StringInterpolator(context, sensitiveDataHolder, input.getBoolean("removeEmptyValueLines", false), recursively);
    }

    public Map<String, String> asProperties(Map<String, Object> props) throws IOException {
        Interpolator interpolator = new ConcordExpressionInterpolator(new StringInterpolator(context, sensitiveDataHolder));

        Map<String, String> result = new LinkedHashMap<>();
        for (var e : props.entrySet()) {
            var key = e.getKey();
            String value = null;
            if (e.getValue() != null) {
                value = interpolator.eval(String.valueOf(e.getValue()), Map.of(), false);
            }
            result.put(key, value);
        }
        return result;
    }

    @Deprecated
    public Map<String, String> asProperties(Map<String, Object> props, boolean skipUndefinedVars) {
        var evalContext = EvalContext.builder()
                .context(context)
                .variables(new ContextVariables(context))
                .undefinedVariableAsNull(true)
                .build();

        var ee = context.execution().runtime().getService(ExpressionEvaluator.class);

        Map<String, String> result = new LinkedHashMap<>();
        for (var e : props.entrySet()) {
            var key = e.getKey();
            var value = ee.eval(evalContext, String.valueOf(e.getValue()), String.class);
            result.put(key, value != null ? value : String.valueOf(e.getValue()));
        }
        return result;
    }

    public Object asJson(String path, Map<String, Object> args) throws Exception {
        try (var in = Files.newInputStream(normalizePath(path))) {
            var result = createObjectMapper(false).readValue(in, Object.class);
            return new ExpressionInterpolator(context).eval(result, args, Object.class, false);
        } catch (Exception e) {
            log.error("Error interpolating json '{}': {}", path, e.getMessage());
            throw e;
        }
    }

    public Object asYaml(String path, Map<String, Object> args) throws Exception {
        try (var in = Files.newInputStream(normalizePath(path))) {
            var result = createYamlObjectMapper(true).readValue(in, Object.class);
            return new ExpressionInterpolator(context).eval(result, args, Object.class, false);
        } catch (Exception e) {
            log.error("Error interpolating yaml '{}': {}", path, e.getMessage());
            throw e;
        }
    }

    @SuppressWarnings("rawtypes")
    public Map evalMap(Map m) {
        return evalMap(m, Map.of(), false);
    }

    public Map<?, ?> evalMap(Map<?, ?> m, Map<String, Object> args) {
        return evalMap(m, args, false);
    }

    public Map<?, ?> evalMap(Map<?, ?> m, Map<String, Object> args, boolean recursively) {
        return new ExpressionInterpolator(context).eval(m, args, Map.class, recursively);
    }

    public String asString(String path) throws Exception {
        return context.eval(Files.readString(normalizePath(path)), null, String.class);
    }

    public String asString(String path, Map<String, Object> args) throws Exception {
        return context.eval(Files.readString(normalizePath(path)), args, String.class);
    }

    private static ObjectMapper createObjectMapper(boolean removeEmptyValues) {
        var om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        if (removeEmptyValues) {
            om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            om.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        }
        return om;
    }

    private static ObjectMapper createYamlObjectMapper(boolean removeEmptyValues) {
        var yamlFactory = new YAMLFactory();
        yamlFactory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        yamlFactory.disable(YAMLGenerator.Feature.SPLIT_LINES);

        var om = new ObjectMapper(yamlFactory);
        if (removeEmptyValues) {
            om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            om.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        }
        return om;
    }

    private Path normalizePath(String path) {
        var p = Paths.get(path);
        if (p.isAbsolute()) {
            return p;
        }
        return context.workingDirectory().resolve(path);
    }

    interface Interpolator {

        String eval(String str, Map<String, Object> additionalArgs, boolean maskSensitiveData) throws IOException;
    }

    static class ConcordExpressionInterpolator implements Interpolator {

        private final Interpolator delegate;

        ConcordExpressionInterpolator(Interpolator delegate) {
            this.delegate = delegate;
        }

        @Override
        public String eval(String string, Map<String, Object> additionalArgs, boolean maskSensitiveData) throws IOException {
            var expressions = ExpressionExtractor.collectExpressions(string);
            for (var expr : expressions) {
                var result = delegate.eval(toConcordExpression(expr), additionalArgs, maskSensitiveData);
                log.warn("interpolate expr '{}' -> null result", expr);
                string = string.replace(expr, result != null ? result : "");
            }
            return string;
        }

        private static String toConcordExpression(String expr) {
            return "${" + expr.substring(EXPR_PREFIX.length());
        }
    }

    static class StringInterpolator implements Interpolator {

        private final Context context;
        private final boolean removeEmptyValueLines;
        private final boolean recursively;
        private final SensitiveDataHolder sensitiveDataHolder;

        StringInterpolator(Context context, SensitiveDataHolder sensitiveDataHolder) {
            this(context, sensitiveDataHolder, false, false);
        }

        StringInterpolator(Context context, SensitiveDataHolder sensitiveDataHolder, boolean removeEmptyValueLines, boolean recursively) {
            this.context = context;
            this.sensitiveDataHolder = sensitiveDataHolder;
            this.removeEmptyValueLines = removeEmptyValueLines;
            this.recursively = recursively;
        }

        @Override
        public String eval(String string, Map<String, Object> additionalArgs, boolean maskSensitiveData) {
            var result = new ExpressionInterpolator(context).eval(string, additionalArgs, String.class, recursively);

            if (maskSensitiveData) {
                result = SensitiveDataUtils.hideSensitiveData(sensitiveDataHolder, result);
            }

            if (removeEmptyValueLines) {
                return YamlValuesCleaner.removeEmptyValueLines(result);
            }

            return result;
        }
    }

    static class ObjectMapperInterpolator implements Interpolator {

        private final Context context;
        private final ObjectMapper mapper;
        private final boolean recursively;
        private final SensitiveDataHolder sensitiveDataHolder;

        ObjectMapperInterpolator(Context context, SensitiveDataHolder sensitiveDataHolder, ObjectMapper mapper, boolean recursively) {
            this.context = context;
            this.sensitiveDataHolder = sensitiveDataHolder;
            this.mapper = mapper;
            this.recursively = recursively;
        }

        @Override
        public String eval(String content, Map<String, Object> additionalArgs, boolean maskSensitiveData) throws IOException {
            var result = mapper.readValue(content, Object.class);
            result = new ExpressionInterpolator(context).eval(result, additionalArgs, Object.class, recursively);
            if (maskSensitiveData) {
                result = SensitiveDataUtils.hideSensitiveData(sensitiveDataHolder, result);
            }
            return writeAsString(mapper, result);
        }

        private static String writeAsString(ObjectMapper mapper, Object value) {
            try {
                return mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(value);
            }
            catch (Exception e) {
                throw new RuntimeException("Error writing value '" + value + "' to string: " + e.getMessage());
            }
        }
    }

    static class JsonInterpolator extends ObjectMapperInterpolator {
        JsonInterpolator(Context context, SensitiveDataHolder sensitiveDataHolder, boolean recursively, boolean removeEmptyValues) {
            super(context, sensitiveDataHolder, createObjectMapper(removeEmptyValues), recursively);
        }
    }

    static class YamlInterpolator extends ObjectMapperInterpolator {

        YamlInterpolator(Context context, SensitiveDataHolder sensitiveDataHolder, boolean recursively, boolean removeEmptyValues) {
            super(context, sensitiveDataHolder, createYamlObjectMapper(removeEmptyValues), recursively);
        }
    }
}
