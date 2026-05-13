package ca.vanzyl.ck8s.utils;

import ca.vanzyl.ck8s.MockTestContext;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class YamlTaskTest {

    @Test
    public void testIndentArray() {
        MockTestContext ctx = new MockTestContext(Collections.emptyMap()) {
            @Override
            public Path workingDirectory() {
                return Path.of(System.getProperty("java.io.tmpdir"));
            }
        };
        YamlTask task = new YamlTask(ctx);

        String nullValueResult = task.indentArray(null, 2);
        assertEquals("", nullValueResult);

        String stringValueResult = task.indentArray("abc", 2);
        assertEquals("  - \"abc\"", stringValueResult);

        String arrValueResult = task.indentArray(Collections.singletonList("abc"), 2);
        assertEquals("  - \"abc\"", arrValueResult);

        String arrValueResult2 = task.indentArray(Arrays.asList("one", "two"), 2);
        assertEquals("  - \"one\"\n  - \"two\"", arrValueResult2);
    }
}
