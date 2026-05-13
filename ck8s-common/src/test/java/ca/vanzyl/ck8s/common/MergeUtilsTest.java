package ca.vanzyl.ck8s.common;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MergeUtilsTest
{

    @Test
    public void mergeTest()
    {
        Map<String, Object> m1 = new HashMap<>();
        m1.put("a", "a-value1");
        m1.put("b", "b-value1");
        m1.put("c", Collections.singletonList("c-value1"));
        m1.put("e", Map.of("k", "v"));

        Map<String, Object> m2 = new HashMap<>();
        m2.put("a", "a-value2");
        m2.put("c", "b-value2");
        m2.put("d", "d-value2");
        m2.put("e", Map.of("k", "v2"));

        Map<String, Object> result = MergeUtils.merge(m1, m2);
        assertEquals("a-value2", result.get("a"));
        assertEquals("b-value1", result.get("b"));
        assertEquals("b-value2", result.get("c"));
        assertEquals("d-value2", result.get("d"));
        assertEquals(Map.of("k", "v2"), result.get("e"));
    }

    @Test
    public void mergeWithNullValue() {
        Map<String, Object> m1 = new HashMap<>();
        m1.put("a", "a-value1");

        Map<String, Object> m2 = new HashMap<>();
        m2.put("a", null);

        Map<String, Object> result = MergeUtils.merge(m1, m2);
        assertNull(result.get("a"));
    }

    @Test
    public void mergeWithNullMapValue() {
        Map<String, Object> m1 = new HashMap<>();
        m1.put("a", Map.of("aa", "a-value1"));

        Map<String, Object> m2 = new HashMap<>();
        m2.put("a", null);

        Map<String, Object> result = MergeUtils.merge(m1, m2);
        assertNull(result.get("a"));
    }
}
