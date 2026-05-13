package ca.vanzyl.ck8s.utils;

import org.junit.Test;

import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class TriggerUtilsTaskTest {

    @Test
    public void testMatch1() {
        List<String> files = Arrays.asList("ck8s-components/file.txt");

        Set<String> result = new TriggerUtilsTask().matchFlows(files, getResourcePath("mappings/mapping.yaml").toString());
        assertEquals(Set.of("itsAep", "itsAdip"), result);
    }

    @Test
    public void testMatch2() {
        List<String> files = Arrays.asList("ck8s-components/aep/1");

        Set<String> result = new TriggerUtilsTask().matchFlows(files, getResourcePath("mappings/mapping.yaml").toString());
        assertEquals(Set.of("itsAep"), result);
    }

    @Test
    public void testMatch3() {
        List<String> files = Arrays.asList("ck8s-components-tests/databricks/1");

        Set<String> result = new TriggerUtilsTask().matchFlows(files, getResourcePath("mappings/mapping.yaml").toString());
        assertEquals(Set.of("itsDatabricks"), result);
    }

    @Test
    public void testMatch4() {
        List<String> files = Arrays.asList("ck8s-orgs/1");

        Set<String> result = new TriggerUtilsTask().matchFlows(files, getResourcePath("mappings/mapping.yaml").toString());
        assertEquals(Set.of("itsAep", "itsDatabricks", "itsAdip"), result);
    }

    @Test
    public void testMatchFiles() {
        Map<String, List<String>> eventFiles = Map.of("any", Arrays.asList("ck8s-orgs/1"));

        Map<String, List<Map<String, Object>>> triggerDefinitionFiles = Map.of("any",
                List.of(Map.of("root", "configs/")));

        boolean result = new TriggerUtilsTask().matchFiles(triggerDefinitionFiles, eventFiles);
        assertFalse(result);

        eventFiles = Map.of("any", Arrays.asList("configs/1"));

        result = new TriggerUtilsTask().matchFiles(triggerDefinitionFiles, eventFiles);
        assertTrue(result);
    }

    private Path getResourcePath(String resource) {
        URL url = TriggerUtilsTaskTest.class.getClassLoader().getResource(resource);
        try {
            return Path.of(url.toURI());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
