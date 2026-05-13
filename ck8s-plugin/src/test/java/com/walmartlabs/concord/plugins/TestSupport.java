package com.walmartlabs.concord.plugins;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Arrays.stream;
import static org.junit.Assert.fail;

public abstract class TestSupport
{

    protected String basedir;

    @Before
    public void setUp()
    {
        basedir = new File("").getAbsolutePath();
    }

    public File target()
    {
        File target = new File(basedir, "target");
        try {
            Files.createDirectories(target.toPath());
        }
        catch (IOException e) {
            fail("Cannot make target directory " + target);
        }
        return target;
    }

    protected File target(String name)
    {
        File target = new File(basedir, "target/" + name);
        if (!target.getParentFile().exists()) {
            target.getParentFile().mkdirs();
        }
        return target;
    }

    // ------------------------------------------------------------------------------------------------------
    // AWS
    // ------------------------------------------------------------------------------------------------------

    protected String readAsString(String name) throws IOException {
        Path path = Paths.get(basedir, "src/test", name);
        return new String(Files.readAllBytes(path));
    }

    public String directory(String directoryName)
    {
        return new File(target(), directoryName).getAbsolutePath();
    }

    public void touch(String... files)
    {
        stream(files).forEach(file -> {
            try {
                Path path = new File(target(), file).toPath();
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            }
            catch (IOException e) {
                fail("Cannot create file " + file);
            }
        });
    }

    public void delete(String directoryName)
            throws IOException
    {
        FileUtils.deleteDirectory(new File(target(), directoryName));
    }
}
