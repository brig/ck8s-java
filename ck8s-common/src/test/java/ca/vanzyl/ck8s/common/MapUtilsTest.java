package ca.vanzyl.ck8s.common;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class MapUtilsTest
{

    @Test
    public void testSet()
    {
        Map<String, Object> m = new HashMap<>();

        MapUtils.set(m, true, "a.b");

        assertTrue(MapUtils.getBoolean(m, "a.b", false));
    }
}
