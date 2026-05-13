package ca.vanzyl.ck8s;

import ca.vanzyl.ck8s.common.MergeUtils;
import ca.vanzyl.concord.k8s.ImmutablesYamlMapper;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Named("merge")
@DryRunReady
public class MergeTask
        implements Task
{

    private final static Logger log = LoggerFactory.getLogger(MergeTask.class);

    private final ImmutablesYamlMapper mapper = new ImmutablesYamlMapper();

    private final Context context;

    @Inject
    public MergeTask(Context context) {
        this.context = context;
    }

    private static void dump(File file)
    {
        log.info("--- File '{}' content: ---", file.getName());
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                log.info(line);
            }
        }
        catch (Exception e) {
            log.warn("Error dumping file '{}': {}", file.getAbsolutePath(), e.getMessage());
        }
        log.info("------");
    }

    @Override
    public TaskResult execute(Variables input)
    {
        boolean debug = input.getBoolean("debug", context.processConfiguration().debug());
        Map<String, Object> result = Collections.emptyMap();

        List<String> files = input.getList("files", Collections.emptyList());
        if (!files.isEmpty()) {
            result = readFile(files.get(0), debug);

            for (int i = 1; i < files.size(); i++) {
                result = MergeUtils.merge(result, readFile(files.get(i), debug));
            }
        }

        List<Map<String, Object>> objects = input.getList("objects", Collections.emptyList());
        if (!objects.isEmpty()) {
            result = MergeUtils.merge(result, objects.get(0));

            for (int i = 1; i < objects.size(); i++) {
                result = MergeUtils.merge(result, objects.get(i));
            }
        }

        String dest = input.getString("dest");
        if (dest != null) {
            writeFile(dest, result);
        }

        return TaskResult.success()
                .value("content", result);
    }

    public Map<String, Object> objects(Map<String, Object> a, Map<String, Object> b) {
        if (b == null) {
            return a;
        }

        return MergeUtils.merge(a, b);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<Map<String, Object>> arrays(List<Map<String, Object>> a, List<Map<String, Object>> b, String key) {
        if (b == null) {
            return a;
        }

        Map<String, Map<String, Object>> aMap = a.stream()
                .collect(Collectors.toMap(item -> String.valueOf(item.get(key)), item -> item, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        Map<String, Map<String, Object>> bMap = b.stream()
                .collect(Collectors.toMap(item -> String.valueOf(item.get(key)), item -> item, (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        Map<String, Map<String, Object>> result = MergeUtils.merge((Map)aMap, (Map)bMap);
        return new ArrayList<>(result.values());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readFile(String fileName, boolean debug)
    {
        if (Files.notExists(Paths.get(fileName))) {
            log.warn("file '{}' not exists", fileName);
            return Collections.emptyMap();
        }

        File file = new File(fileName);

        if (debug) {
            dump(file);
        }

        try {
            return mapper.read(file, Map.class);
        }
        catch (FileNotFoundException e) {
            log.error("Error reading file '{}': not found", file.getAbsolutePath());
            throw new RuntimeException(e);
        }
        catch (Exception e) {
            log.error("Error reading file '{}': {}", file.getAbsolutePath(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void writeFile(String fileName, Map<String, Object> content)
    {
        Path path = Paths.get(fileName);
        try {
            Files.createDirectories(path.getParent());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            Files.write(path, mapper.write(content).getBytes());
        }
        catch (Exception e) {
            log.error("Error writing file '{}': {}", path.toAbsolutePath(), e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
