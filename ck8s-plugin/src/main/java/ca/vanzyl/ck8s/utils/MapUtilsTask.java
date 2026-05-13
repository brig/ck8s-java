package ca.vanzyl.ck8s.utils;

import ca.vanzyl.ck8s.common.MapUtils;
import com.walmartlabs.concord.runtime.v2.sdk.DryRunReady;
import com.walmartlabs.concord.runtime.v2.sdk.Task;

import javax.inject.Named;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Named("mapUtils")
@DryRunReady
public class MapUtilsTask implements Task {

    public Object get(Map<String, Object> m, String path) {
        if (m == null) {
            return null;
        }

        return MapUtils.getObject(m, path.split("\\."));
    }

    @SuppressWarnings("unchecked")
    public static void removeNullValues(Object object) {
        if (object instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) object;
            Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = iterator.next();
                if (entry.getValue() == null) {
                    iterator.remove();
                } else {
                    removeNullValues(entry.getValue());
                }
            }
        } else if (object instanceof List) {
            List<Object> list = (List<Object>) object;
            list.removeIf(Objects::isNull);
            for (Object item : list) {
                removeNullValues(item);
            }
        }
    }
}
