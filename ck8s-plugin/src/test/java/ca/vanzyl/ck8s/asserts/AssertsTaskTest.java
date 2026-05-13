package ca.vanzyl.ck8s.asserts;

import ca.vanzyl.concord.k8s.ImmutablesYamlMapper;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.FileService;
import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AssertsTaskTest
{

    private static void assertEquals(Object expected, Object actual)
    {
        new AssertsTask(null, null, null).assertEquals(expected, actual);
    }

    private static void assertYamlEquals(Object expected, Object actual) {
        Context ctx = mockContext();

        try (AssertsTask.TemporaryFile tmpFile = new AssertsTask.TemporaryFile(Files.createTempFile("test", "yaml"))) {
            Files.writeString(tmpFile.path(), new ImmutablesYamlMapper().write(expected));
            new AssertsTask(ctx, null, null)
                    .assertYamlEquals(tmpFile.path().toString(), actual, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Context mockContext() {
        Context ctx = mock(Context.class);
        when(ctx.fileService()).thenReturn(new FileService() {
            @Override
            public Path createTempFile(String prefix, String suffix) throws IOException {
                return Files.createTempFile(prefix, suffix);
            }

            @Override
            public Path createTempDirectory(String prefix) {
                throw new RuntimeException("Not implemented");
            }
        });
        return ctx;
    }

    private static void assertYamlNotEquals(Object expected, Object actual) {
        try {
            assertYamlEquals(expected, actual);
            fail("exception expected");
        }
        catch (UserDefinedException e) {
            System.out.println("" + e.getMessage());
            // expected
        }
    }

    private static void assertNotEquals(Object expected, Object actual)
    {
        try {
            new AssertsTask(null, null, null).assertEquals(expected, actual);
            fail("exception expected");
        }
        catch (UserDefinedException e) {
            // expected
        }
    }

    @Test
    public void testAssertEquals()
    {
        assertEquals(1, 1);
        assertEquals(1, 1L);
        assertEquals(1L, 1L);
        assertEquals("abc", "abc");

        assertNotEquals(1, 2);
        assertNotEquals(1L, 2L);
        assertNotEquals(1, 2L);
        assertNotEquals(1L, 2);
        assertNotEquals("1", "2asd");
    }

    @Test
    public void assertYaml1() {
        Map<String, Object> m1 = new HashMap<>();
        m1.put("k", "v");

        Map<String, Object> m2 = new HashMap<>();
        m2.put("k", "v");
        m2.put("k2", "v2");

        assertYamlNotEquals(m1, m2);
    }

    @Test
    public void assertYaml2() {
        Map<String, Object> m1 = new HashMap<>();
        m1.put("k", "v");
        m1.put("k2", Collections.singletonList("one"));

        Map<String, Object> m2 = new HashMap<>();
        m2.put("k", "v");
        m2.put("k2", Arrays.asList("one", "two"));

        assertYamlNotEquals(m1, m2);
    }

    @Test
    public void assertYaml3() {
        List<String> a1 = Arrays.asList("one", "two");
        List<String> a2 = Collections.singletonList("two");

        assertYamlNotEquals(a1, a2);
    }

    @Test
    public void assertJson() throws Exception {
        String actual = Files.readString(resource("/ca/vanzyl/ck8s/asserts/json/002-actual.json"));
        new AssertsTask(mockContext(), null, null)
                .assertJson(resource("/ca/vanzyl/ck8s/asserts/json/002-expected.json").toString(), actual, false);
    }

    private static Path resource(String name) throws Exception
    {
        URL resource = AssertsTaskTest.class.getResource(name);
        assertNotNull(resource);
        return Path.of(resource.toURI());
    }
}
