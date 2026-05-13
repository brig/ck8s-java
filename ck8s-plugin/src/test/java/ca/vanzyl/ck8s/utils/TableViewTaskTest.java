package ca.vanzyl.ck8s.utils;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TableViewTaskTest {

    @Test
    public void test() {
        List<Collection<String>> columns =
                List.of(
                        List.of("mac:3d0f396"),
                        List.of("mac:123456")
                );

        Map<String, List<String>> result = new TableViewTask().transformWithSplit(columns, ":");

        assertEquals(1, result.size());
        assertNotNull(result.get("mac"));
        assertEquals(List.of("3d0f396", "123456"), result.get("mac"));

        System.out.println(TableViewTask.toString(result, "service", "env1", "env2"));
    }

    @Test
    public void testEmpty() {
        List<Collection<String>> columns = List.of();

        Map<String, List<String>> result = new TableViewTask().transform(columns, s -> s.split(":")[0], s -> s.split(":")[1]);

        assertEquals(0, result.size());

        System.out.println(TableViewTask.toString(result, "service", "env1", "env2"));
    }

    @Test
    public void test3() {
        List<Collection<String>> columns =
                List.of(
                        List.of("mac:3d0f396"),
                        List.of("mac:123456", "mac2:777")
                );

        Map<String, List<String>> result = new TableViewTask().transform(columns, s -> s.split(":")[0], s -> s.split(":")[1]);

        assertEquals(2, result.size());
        assertNotNull(result.get("mac"));
        assertEquals(List.of("3d0f396", "123456"), result.get("mac"));

        assertNotNull(result.get("mac2"));
        assertEquals(Arrays.asList(null, "777"), result.get("mac2"));

        System.out.println(TableViewTask.toString(result, "service", "env1", "env2"));
    }

    @Test
    public void test4() {
        List<Collection<String>> columns =
                List.of(
                        List.of(),
                        List.of("mac:123456", "mac2:777")
                );

        Map<String, List<String>> result = new TableViewTask().transform(columns, s -> s.split(":")[0], s -> s.split(":")[1]);

        assertEquals(2, result.size());
        assertNotNull(result.get("mac"));
        assertEquals(Arrays.asList(null, "123456"), result.get("mac"));

        assertNotNull(result.get("mac2"));
        assertEquals(Arrays.asList(null, "777"), result.get("mac2"));

        System.out.println(TableViewTask.toString(result, "service", "env1", "env2"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testWithEmpty() {
        String a = "mac:123456";

        String b = "mac:22222\n" +
                   "list:99c8d0b";

        String c = "mac:123456";

        List<Collection<String>> allItems = DiffTask.normalizeInput(List.of(a, b, c));

        List<Collection<String>> columns = new DiffTask().compute((List)allItems);

        Map<String, List<String>> result = new TableViewTask()
                .transform(columns, s -> s.split(":")[0], s -> s.split(":")[1], allItems);

        assertEquals(List.of("123456", "22222", "123456"), result.get("mac"));
        assertEquals(Arrays.asList(null, "99c8d0b", null), result.get("list"));
    }
}
