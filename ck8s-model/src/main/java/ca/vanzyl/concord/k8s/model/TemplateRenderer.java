package ca.vanzyl.concord.k8s.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TemplateRenderer
{

    private static final Logger logger = LoggerFactory.getLogger(TemplateRenderer.class);

    private final String serverImportsDirectory;
    private final MustacheFactory mustacheFactory;

    public TemplateRenderer()
    {
        this(null);
    }

    public TemplateRenderer(String serverImportsDirectory)
    {
        this.serverImportsDirectory = serverImportsDirectory;
        this.mustacheFactory = createMustacheFactory();
    }

    public static String version()
    {
        Properties projectProperties = new Properties();
        try (InputStream is = TemplateRenderer.class.getClassLoader().getResourceAsStream("project.properties")) {
            projectProperties.load(is);
            return projectProperties.getProperty("version");
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot determine the version of the K8s system from the project.properties file.", e);
        }
    }

    private static MustacheFactory createMustacheFactory()
    {
        DefaultMustacheFactory mustacheFactory = new DefaultMustacheFactory();
        mustacheFactory.setObjectHandler(new MethodAccessingObjectHandler());
        return mustacheFactory;
    }

    public String render(K8sRequest request, TemplateRendererOptions options)
            throws IOException
    {
        String templateName = String.format("%s/%s.mustache", request.metadata().profile(), options.template());
        return render(Collections.singletonMap("cluster", request), templateName, options);
    }

    public String render(Map<String, Object> variables, String templateName, TemplateRendererOptions options)
            throws IOException
    {
        // automatically provided variables
        variables = new HashMap<>(variables != null ? variables : Collections.emptyMap());
        variables.put("k8sSystem", Collections.singletonMap("version", version()));
        variables.put("options", new ObjectMapper().convertValue(options, Map.class));

        Mustache mustache = mustacheFactory.compile(getReader(templateName), templateName);
        Writer writer = new StringWriter();
        mustache.execute(writer, variables);
        return writer.toString();
    }

    private Reader getReader(String template)
            throws IOException
    {
        // TODO(ib): do we need this? Except for the tests

        if (serverImportsDirectory != null) {
            // There may be imports available on the server so we will process the if they are available. Imports
            // might have been built into a Concord server docker images, or they may be mounted into the
            // container for local development purposes.
            Path imports = Paths.get(serverImportsDirectory);
            if (Files.exists(imports)) {
                Path templatePath = imports.resolve(template);
                if (Files.exists(templatePath)) {
                    logger.info("Using server side import template: {}", template);
                    return Files.newBufferedReader(templatePath);
                }
            }
        }

        return mustacheFactory.getReader("imports/" + template);
    }

    private static class MethodAccessingObjectHandler
            extends ReflectionObjectHandler
    {

        @Override
        protected boolean areMethodsAccessible(Map<?, ?> map)
        {
            return true;
        }
    }
}
