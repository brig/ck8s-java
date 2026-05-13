package ca.vanzyl.concord.k8s.model;

import ca.vanzyl.concord.k8s.ImmutablesYamlMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.function.Function;

public class K8sClusterDeploymentReader
{

    private final ImmutablesYamlMapper mapper;

    public K8sClusterDeploymentReader()
    {
        this.mapper = new ImmutablesYamlMapper();
    }

    public K8sClusterDeployment read(File file)
            throws IOException
    {
        return read(file, request -> request);
    }

    public K8sClusterDeployment read(
            File file,
            Function<K8sClusterDeployment, K8sClusterDeployment> srcTemplateUpdater)
            throws IOException
    {
        String fileContent = new String(Files.readAllBytes(file.toPath()));
        // Create initial cluster provisioning request
        K8sClusterDeployment request =
                srcTemplateUpdater.apply(
                        mapper.read(file, ImmutableK8sClusterDeployment.class));

        // Interpolate the cluster provisioning request against itself
        Writer writer = new StringWriter();
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(new StringReader(fileContent), "cluster-provisioning-request");
        mustache.execute(writer, request);
        writer.flush();

        return mapper.read(writer.toString(), K8sClusterDeployment.class);
    }

    public String write(K8sClusterDeployment request)
            throws IOException
    {
        return mapper.write(request);
    }
}
