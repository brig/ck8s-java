package ca.vanzyl.ck8s.utils;

import java.util.Map;

// Inspecting the Concord context and extracting a value from a path like ${clusterRequest.ingress.annotations.external}

public class MapUtils
{

    public static Map<String, String> clusterRequestMap(Map<String, Object> clusterRequest, String path)
    {
        return (Map<String, String>) find(clusterRequest, path);
    }

    public static String clusterRequestString(Map<String, Object> clusterRequest, String path)
    {
        return (String) find(clusterRequest, path);
    }

    public static boolean clusterRequestBoolean(Map<String, Object> clusterRequest, String path)
    {
        Object result = find(clusterRequest, path);
        if (result == null) {
            return false;
        }
        return (boolean) find(clusterRequest, path);
    }

    public static Object find(Map<String, Object> clusterRequest, String path)
    {
        String[] segments = path.split("\\.");
        Map<String, Object> segment = (Map<String, Object>) clusterRequest.get(segments[0]);
        for (int i = 1; i < segments.length - 1; i++) {
            if (segment == null) {
                return null;
            }
            segment = (Map<String, Object>) segment.get(segments[i]);
        }
        return segment.get(segments[segments.length - 1]);
    }
}
