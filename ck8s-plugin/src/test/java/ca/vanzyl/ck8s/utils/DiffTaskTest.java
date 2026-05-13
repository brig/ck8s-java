package ca.vanzyl.ck8s.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DiffTaskTest {

    @Test
    public void testDiff() {
        String a =
                "mac:3d0f396\n" +
                "list:99c8d0b";

        String b =
                "mac:123456\n" +
                "list:99c8d0b";

        List<Collection<String>> result = new DiffTask().compute(a, b);

        assertEquals(2, result.size());
        assertEquals(List.of("mac:3d0f396"), new ArrayList<>(result.get(0)));
        assertEquals(List.of("mac:123456"), new ArrayList<>(result.get(1)));
    }

    @Test
    public void testDiffWithEmpty() {
        String a =
                "";

        String b =
                "mac:123456\n" +
                "list:99c8d0b";

        List<Collection<String>> result = new DiffTask().compute(a, b);

        assertEquals(2, result.size());
        assertEquals(List.of(), new ArrayList<>(result.get(0)));
        assertEquals(List.of("mac:123456", "list:99c8d0b"), new ArrayList<>(result.get(1)));
    }

    @Test
    public void testNoDiff() {
        String a =
                "mac:123456\n" +
                "list:99c8d0b";

        String b =
                "mac:123456\n" +
                "list:99c8d0b";

        List<Collection<String>> result = new DiffTask().compute(a, b);

        assertEquals(List.of(), result);
    }

    @Test
    public void testDiff1() {
        String a =
                "mac:123456";

        String b =
                "mac:123456\n" +
                "list:99c8d0b";

        List<Collection<String>> result = new DiffTask().compute(a, b);

        assertEquals(2, result.size());
        assertEquals(0, result.get(0).size());
        assertEquals(List.of("list:99c8d0b"), new ArrayList<>(result.get(1)));
    }

    @Test
    public void testDiff3Lists() {
        String a =
                "mac:123456";

        String b =
                "mac:123456\n" +
                "list:99c8d0b";

        String c =
                "mac:123456";

        List<Collection<String>> result = new DiffTask().compute(a, b, c);

        assertEquals(3, result.size());
        assertEquals(0, result.get(0).size());
        assertEquals(List.of("list:99c8d0b"), new ArrayList<>(result.get(1)));
    }
}
