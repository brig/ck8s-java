package ca.vanzyl.ck8s.utils;

import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class CollectionsTaskTest {

    @Test
    public void testConcat() {
        List<Object> result = CollectionsTask.concat(List.of(1, 2, 3), List.of(4, 5, 6));

        assertEquals(List.of(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testConcatWithNull() {
        List<Object> result = CollectionsTask.concat(null, List.of(4, 5, 6));

        assertEquals(List.of(4, 5, 6), result);
    }

    @Test
    public void testConcatAsSet() {
        Set<Object> result = CollectionsTask.concatAsSet(List.of(1, 2, 3), List.of(1, 2, 3, 4, 5, 6));

        assertEquals(Set.of(1, 2, 3, 4, 5, 6), result);
    }
}
