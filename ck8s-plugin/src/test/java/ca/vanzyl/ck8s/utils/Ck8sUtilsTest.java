package ca.vanzyl.ck8s.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class Ck8sUtilsTest {

    @Test
    public void testListToMap() {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(Map.of("id", "one", "value", 1));
        items.add(Map.of("id", "two", "value", 2));

        Map<String, Object> result = Ck8sUtils.listToMap(items, "id", "value");

        assertEquals(2, result.size());
        assertEquals(1, result.get("one"));
        assertEquals(2, result.get("two"));
    }
}
