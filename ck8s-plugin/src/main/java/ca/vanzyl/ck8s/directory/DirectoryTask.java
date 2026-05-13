package ca.vanzyl.ck8s.directory;

import com.walmartlabs.concord.runtime.v2.sdk.DryRunReady;
import com.walmartlabs.concord.runtime.v2.sdk.Task;

import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Named("directoryTool")
@DryRunReady
public class DirectoryTask
        implements Task
{

    public boolean exists(String directoryName)
    {
        if (directoryName == null || directoryName.trim().isEmpty()) {
            return false;
        }

        return Files.exists(Paths.get(directoryName));
    }

    public List<String> manifests(String directoryName)
            throws IOException
    {
        return DirectoryUtils.manifests(directoryName);
    }

    public List<String> scan(String directoryName)
            throws IOException
    {
        return DirectoryUtils.scan(directoryName, "**/**", null);
    }
}
