package ca.vanzyl.ck8s.model.v1;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

public class IoTest
{

    @Test
    @Ignore
    public void validateIo()
            throws Exception
    {
        Io io = new Io();
        Cluster cluster = io.read(new File("/Users/jason.vanzyl/mcp-infra/v1/concord-dev0.yaml"));
        String clusterYaml = io.write(cluster);
        System.out.println(clusterYaml);
    }
}
