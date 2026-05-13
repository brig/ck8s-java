package ca.vanzyl.ck8s.utils;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class MapUtilsTaskTest {

    @Test
    public void testRemoveNullValuesFromNestedMap() {
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("nestedKey1", null);
        nestedMap.put("nestedKey2", "nestedValue2");

        Map<String, Object> map = new HashMap<>();
        map.put("key3", nestedMap);

        MapUtilsTask.removeNullValues(map);

        @SuppressWarnings("unchecked")
        Map<String, Object> resultNestedMap = (Map<String, Object>) map.get("key3");

        assertNotNull(resultNestedMap);
        assertFalse(resultNestedMap.containsValue(null));
        assertTrue(resultNestedMap.containsKey("nestedKey2"));
        assertEquals(1, resultNestedMap.size());
    }

    @Test
    public void testRemoveNullValuesFromNestedList() {
        List<Object> nestedList = new ArrayList<>(Arrays.asList("listValue1", null, "listValue3"));

        Map<String, Object> map = new HashMap<>();
        map.put("key4", nestedList);

        MapUtilsTask.removeNullValues(map);

        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) map.get("key4");

        assertNotNull(resultList);
        assertFalse(resultList.contains(null));
        assertEquals(2, resultList.size());
    }
}
