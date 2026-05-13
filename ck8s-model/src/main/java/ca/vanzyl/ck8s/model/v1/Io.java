package ca.vanzyl.ck8s.model.v1;

import ca.vanzyl.concord.k8s.ImmutablesYamlMapper;

import java.io.File;
import java.io.IOException;

public class Io
{

    private final ImmutablesYamlMapper mapper = new ImmutablesYamlMapper();

    public Cluster read(File file)
            throws IOException
    {
        return mapper.read(file, Cluster.class);
    }

    public String write(Cluster cluster)
            throws IOException
    {
        return mapper.write(cluster);
    }
}
