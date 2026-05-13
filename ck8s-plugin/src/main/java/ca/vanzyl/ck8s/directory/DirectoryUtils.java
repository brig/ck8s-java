package ca.vanzyl.ck8s.directory;

import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DirectoryUtils
{

    public static List<String> manifests(String directoryName)
            throws IOException
    {
        return scan(directoryName, "**/*.yaml", "test/**");
    }

    public static List<String> scan(String directoryName, String includes, String excludes)
            throws IOException
    {
        List<String> fileNames = new ArrayList<>(FileUtils.getFileNames(new File(directoryName), includes, excludes, true));
        Collections.sort(fileNames);
        return fileNames;
    }
}
