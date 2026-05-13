package ca.vanzyl.ck8s.python;

import ca.vanzyl.ck8s.MockTestContext;
import ca.vanzyl.ck8s.aws.CredentialsProvider;
import ca.vanzyl.ck8s.command.CliCommand;
import com.walmartlabs.concord.runtime.v2.runner.DefaultFileService;
import com.walmartlabs.concord.runtime.v2.sdk.FileService;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import com.walmartlabs.concord.runtime.v2.sdk.WorkingDirectory;
import nl.altindag.log.LogCaptor;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class PythonTaskTest {
    private final PythonTask task = new PythonTask(new PythonTestContext(), mock(CredentialsProvider.class));
    private static final Logger log = LoggerFactory.getLogger(PythonTaskTest.class);

    @Test
    public void missingArguments()
            throws Exception {
        try {
            task.execute(new MapBackedVariables(Map.of()));
            fail("Exception not thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("Mandatory variable 'script' is required", e.getMessage());
        }

    }

    @Test
    public void argumentsWithSpaces()
            throws Exception {
        cleanVenv("/python/test-python-single-argument.py");
        assertTrue(
                task.execute(new MapBackedVariables(Map.of(
                        "script",
                        Objects.requireNonNull(
                                getClass().getResource("/python/test-python-single-argument.py")).toURI().getPath(),
                        "args", List.of("whitespace argument")
                ))).ok());
    }

    @Test
    public void argumentsWithSpacesNegative()
            throws Exception {
        cleanVenv("/python/test-python-single-argument.py");
        assertFalse(
                task.execute(new MapBackedVariables(Map.of(
                        "script",
                        Objects.requireNonNull(
                                getClass().getResource("/python/test-python-single-argument.py")).toURI().getPath(),
                        "args", List.of("whitespace", "argument")
                ))).ok());
    }

    @Test
    public void inVenv()
            throws Exception {
        cleanVenv("/python/test-python-venv.py");
        assertTrue(
                task.execute(new MapBackedVariables(Map.of(
                        "script",
                        Objects.requireNonNull(
                                getClass().getResource("/python/test-python-venv.py")).toURI().getPath()
                ))).ok());
    }

    @Test
    public void inVenvNegative()
            throws Exception {
        cleanVenv("/python/test-python-venv.py");
        assertFalse(
                task.execute(new MapBackedVariables(Map.of(
                        "script",
                        Objects.requireNonNull(
                                getClass().getResource("/python/test-python-venv.py")).toURI().getPath(),
                        "venv",
                        false
                ))).ok());
    }

    @Test
    public void importRequirements()
            throws Exception {
        cleanVenv("/python/with-requirements/test-python-requirements.py");
        String path = PythonTaskTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        System.out.println("PATH " + path + "/python/with-requirements/.venv");
        assertTrue(
                task.execute(new MapBackedVariables(Map.of(
                        "script",
                        Objects.requireNonNull(
                                getClass().getResource("/python/with-requirements/test-python-requirements.py")).toURI().getPath()
                ))).ok());
    }

    @Test
    public void reuseVenv()
            throws Exception {
        cleanVenv("/python/with-requirements/test-python-requirements.py");
        cleanVenv("/python/test-python-venv.py");
        LogCaptor logCaptor = LogCaptor.forClass(CliCommand.class);
        assertTrue(
                task.execute(new MapBackedVariables(Map.of(
                        "script",
                        Objects.requireNonNull(
                                getClass().getResource("/python/with-requirements/test-python-requirements.py")).toURI().getPath()
                ))).ok());
        assertTrue(logCaptor.getInfoLogs().stream().anyMatch(s -> s.contains("Creating new reusable venv")));
        logCaptor.clearLogs();

        assertTrue(
                task.execute(new MapBackedVariables(Map.of(
                        "script",
                        Objects.requireNonNull(
                                getClass().getResource("/python/with-requirements/test-python-requirements.py")).toURI().getPath()
                ))).ok());

        assertTrue(logCaptor.getInfoLogs().stream().anyMatch(s -> s.contains("Found existing venv")));
        logCaptor.clearLogs();

        assertTrue(
                task.execute(new MapBackedVariables(Map.of(
                        "script",
                        Objects.requireNonNull(
                                getClass().getResource("/python/test-python-venv.py")).toURI().getPath()
                ))).ok());
        assertTrue(logCaptor.getInfoLogs().stream().anyMatch(s -> s.contains("Creating new reusable venv")));
        logCaptor.close();
    }

    @Test
    public void reuseVenvParallel()
            throws Exception {
        cleanVenv("/python/with-requirements/test-python-requirements.py");
        LogCaptor logCaptor = LogCaptor.forClass(CliCommand.class);
        List<Integer> parallel = List.of(1, 2);
        List<Boolean> result = parallel.parallelStream().map(i -> {
            try {
                return task.execute(new MapBackedVariables(Map.of(
                        "script",
                        Objects.requireNonNull(
                                getClass().getResource("/python/with-requirements/test-python-requirements.py")).toURI().getPath()
                ))).ok();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList();
        assertArrayEquals(new Boolean[]{true, true}, result.toArray());
        assertEquals(2, logCaptor.getInfoLogs().stream().filter(s -> s.contains("Installing dependencies")).count());
        assertEquals(1, logCaptor.getInfoLogs().stream().filter(s -> s.contains("Found existing venv")).count());
        assertEquals(1, logCaptor.getInfoLogs().stream().filter(s -> s.contains("Creating new reusable venv")).count());
        List<String> venvLockLogs = logCaptor.getInfoLogs().stream().filter(s -> s.contains(".venv.lock")).toList();
        List<String> depsLockLogs = logCaptor.getInfoLogs().stream().filter(s -> s.contains(".deps-lock")).toList();
        assertLocksOrdering(venvLockLogs);
        assertLocksOrdering(depsLockLogs);
        logCaptor.close();
    }

    private void assertLocksOrdering(List<String> lockLogs) {
        int locks = 0;
        for (String log : lockLogs) {
            if (log.contains("Obtained lock")) {
                locks++;
            } else if (log.contains("Released lock")) {
                locks--;
            }
            assertTrue(locks >= 0);
            assertTrue(locks <= 1);
        }
        assertEquals(0, locks);
    }

    @Test
    public void noReuseVenv()
            throws Exception {
        cleanVenv("/python/with-requirements/test-python-requirements.py");
        LogCaptor logCaptor = LogCaptor.forClass(CliCommand.class);
        assertTrue(
                task.execute(new MapBackedVariables(Map.of(
                        "script",
                        Objects.requireNonNull(
                                getClass().getResource("/python/with-requirements/test-python-requirements.py")).toURI().getPath(),
                        "reuseVenv",
                        false
                ))).ok());
        assertTrue(logCaptor.getInfoLogs().stream().anyMatch(s -> s.contains("Creating new venv")));
        logCaptor.clearLogs();

        assertTrue(
                task.execute(new MapBackedVariables(Map.of(
                        "script",
                        Objects.requireNonNull(
                                getClass().getResource("/python/with-requirements/test-python-requirements.py")).toURI().getPath(),
                        "reuseVenv",
                        false
                ))).ok());
        assertTrue(logCaptor.getInfoLogs().stream().anyMatch(s -> s.contains("Creating new venv")));
        logCaptor.close();
    }

    @Test
    public void reuseNamedVenv()
            throws Exception {
        cleanVenv("/python/with-requirements/test-python-requirements.py");
        LogCaptor logCaptor = LogCaptor.forClass(CliCommand.class);
        String venvName = "test-venv-" + UUID.randomUUID();
        assertTrue(
                task.execute(new MapBackedVariables(Map.of(
                        "script",
                        Objects.requireNonNull(
                                getClass().getResource("/python/with-requirements/test-python-requirements.py")).toURI().getPath(),
                        "venvName",
                        venvName
                ))).ok());
        assertTrue(logCaptor.getInfoLogs().stream().anyMatch(s -> s.contains("Creating new reusable venv") && s.contains(venvName)));
        logCaptor.clearLogs();

        assertTrue(
                task.execute(new MapBackedVariables(Map.of(
                        "script",
                        Objects.requireNonNull(
                                getClass().getResource("/python/with-requirements/test-python-requirements.py")).toURI().getPath()
                ))).ok());
        assertTrue(logCaptor.getInfoLogs().stream().anyMatch(s -> s.contains("Creating new reusable venv")));
        logCaptor.clearLogs();

        assertTrue(
                task.execute(new MapBackedVariables(Map.of(
                        "script",
                        Objects.requireNonNull(
                                getClass().getResource("/python/test-python-venv.py")).toURI().getPath(),
                        "venvName",
                        venvName
                ))).ok());
        assertTrue(logCaptor.getInfoLogs().stream().anyMatch(s -> s.contains("Found existing venv") && s.contains(venvName)));
        logCaptor.close();
    }

    @Test
    public void importUsingPyprojectToml()
            throws Exception {
        cleanVenv("/python/with-pyproject-toml/test-python-pyproject-toml.py");
        assertTrue(
                task.execute(new MapBackedVariables(Map.of(
                        "script",
                        Objects.requireNonNull(
                                getClass().getResource("/python/with-pyproject-toml/test-python-pyproject-toml.py")).toURI().getPath()
                ))).ok());
    }

    @Test
    @Ignore
    public void importRequirementsNegative()
            throws Exception {
        cleanVenv("/python/with-requirements/test-python-requirements.py");
        assertFalse(
                task.execute(new MapBackedVariables(Map.of(
                        "script",
                        Objects.requireNonNull(
                                getClass().getResource("/python/test-python-missing-requirements.py")).toURI().getPath()
                ))).ok());
    }

    static class PythonTestContext extends MockTestContext {
        PythonTestContext() {
            super(Map.of("clusterRequest", Collections.singletonMap("envars", Collections.emptyMap())));
        }

        @Override
        public Path workingDirectory() {
            return Path.of(System.getProperty("user.dir"));
        }

        @Override
        public FileService fileService() {
            return new DefaultFileService(new WorkingDirectory(workingDirectory()));
        }
    }

    private void cleanVenv(String scriptPath)
            throws Exception {
        String testClassPath = PythonTaskTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        Path venv = Path.of(testClassPath + scriptPath).getParent().resolve(".venv");
        if (Files.exists(venv)) {
            log.info("Cleaning venv: {}", venv);
            Files.walk(venv)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }

    }
}
