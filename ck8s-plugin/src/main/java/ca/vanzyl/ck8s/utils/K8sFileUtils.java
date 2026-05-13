package ca.vanzyl.ck8s.utils;

import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.DryRunReady;
import com.walmartlabs.concord.runtime.v2.sdk.FileService;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static java.lang.String.join;
import static java.nio.file.Files.readAllLines;
import static java.nio.file.Paths.get;
import static java.util.Comparator.reverseOrder;

@Named("k8sFileUtils")
@DryRunReady
public class K8sFileUtils
        implements Task
{

    private static final Logger log = LoggerFactory.getLogger(K8sFileUtils.class);
    private static final Logger processLog = LoggerFactory.getLogger("processLog");

    private final FileService fileService;
    private final Path workDir;

    @Inject
    public K8sFileUtils(Context context)
    {
        this.fileService = context.fileService();
        this.workDir = context.workingDirectory();
    }

    public void deleteDirectory(String directory)
            throws Exception
    {
        Path directoryToDelete = get(directory);
        if (Files.exists(directoryToDelete)) {
            Files.walk(directoryToDelete)
                    .sorted(reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    public void createDirectory(String directory) throws IOException {
        Files.createDirectories(Path.of(directory));
    }

    public String joinLinesFromFile(String delimiter, String path)
            throws IOException
    {
        return join(delimiter, readAllLines(get(path)));
    }

    public String duplicateFile(String src)
            throws IOException
    {
        Path srcPath = Paths.get(src);
        Path dstPath = fileService.createTempFile("k8sFileUtils-duplicate", ".tmp");
        Files.copy(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING);
        return dstPath.toAbsolutePath().toString();
    }

    public String tempFile(String prefix, String suffix)
            throws IOException
    {
        Path result = fileService.createTempFile(prefix, suffix);
        return relativeToWorkdir(result.toAbsolutePath().toString());
    }

    public void copyFile(String src, String dest)
            throws IOException
    {
        Path srcPath = Paths.get(src);
        if (!srcPath.isAbsolute()) {
            srcPath = workDir.resolve(srcPath);
        }
        Path dstPath = Paths.get(dest);
        if (!dstPath.isAbsolute()) {
            dstPath = workDir.resolve(dstPath);
        }
        Path parent = dstPath.getParent();
        if (Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
        Files.copy(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public List<String> lines(String path) throws IOException {
        return Files.readAllLines(Path.of(path));
    }

    public String relativeToWorkdir(String path)
    {
        return workDir.relativize(Path.of(path)).toString();
    }

    public String toWorkdirPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        if (path.startsWith(workDir.toString())) {
            return path;
        }

        return workDir.resolve(path).toAbsolutePath().normalize().toString();
    }

    public String prettyPrintPath(String path)
    {
        if (path.startsWith(workDir.toString())) {
            return relativeToWorkdir(path);
        }
        else {
            return path;
        }
    }

    public static String addPrefixToFileName(String filePath, String prefix) {
        if (filePath == null) {
            return null;
        }

        if (prefix == null || prefix.trim().isEmpty()) {
            return filePath;
        }

        int lastSeparatorIndex = filePath.lastIndexOf('/');
        if (lastSeparatorIndex == -1) {
            return  prefix + filePath;
        }

        String path = filePath.substring(0, lastSeparatorIndex + 1);
        String filename = filePath.substring(lastSeparatorIndex + 1);
        return path + prefix + filename;
    }

    public static String getFilenameWithoutExtension(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        String fileName = (new File(path)).getName();
        int dotIndex = fileName.lastIndexOf(46);
        return dotIndex == -1 ? fileName : fileName.substring(0, dotIndex);
    }

    public static boolean isEmpty(String pathStr) throws IOException {
        Path path = Paths.get(pathStr);

        if (Files.exists(path) && Files.isRegularFile(path)) {
            return Files.size(path) == 0;
        } else {
            return true;
        }
    }

    public static List<Path> listFiles(String path) throws IOException {
        var dirPath = Paths.get(path);
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            return List.of();
        }

        try (var stream = Files.list(dirPath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .toList();
        }
    }

    public List<String> listAllFilesRecursive(String path) throws IOException {
        var dir = assertWorkDirPath(path);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (var s = Files.walk(dir)) {
            return s.filter(Files::isRegularFile)
                    .map(Path::toString)
                    .toList();
        }
    }

    public Path toPath(String path) throws IOException {
        return Paths.get(path);
    }

    public void dumpFile(String path) {
        var p = assertWorkDirPath(path);
        if (Files.notExists(p)) {
            log.info("File '{}' does not exist", path);
            return;
        }

        try (var lines = Files.lines(p, StandardCharsets.UTF_8)) {
            lines.forEach(line -> {
                if (line.contains("ERROR") || line.contains("Exception ")) {
                    processLog.error("{}", line);
                } else {
                    processLog.info("{}", line);
                }
            });
        } catch (Exception e) {
            log.warn("Failed to read '{}'", path, e);
        }
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
}
