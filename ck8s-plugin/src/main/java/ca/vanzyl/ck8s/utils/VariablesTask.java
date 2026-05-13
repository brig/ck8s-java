package ca.vanzyl.ck8s.utils;

import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.svm.Frame;
import com.walmartlabs.concord.svm.FrameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Named("variables")
public class VariablesTask implements Task {

    private final static Logger log = LoggerFactory.getLogger(VariablesTask.class);

    private final Context ctx;

    @Inject
    public VariablesTask(Context ctx) {
        this.ctx = ctx;
    }

    public void setFrom(Map<String, Object> vars) {
        log.info("setting variables: {}", vars.keySet());

        vars.forEach((k, v) -> {
            ctx.variables().set(k, v);
        });
    }

    public void setGlobal(String key, Object value) {
        List<Frame> rootFrames = ctx.execution().state().getFrames(ctx.execution().state().getRootThreadId());
        if (rootFrames.isEmpty()) {
            throw new RuntimeException("Root frames is empty");
        }

        Frame f = rootFrames.get(rootFrames.size() - 1);
        if (f.getType() != FrameType.ROOT) {
            throw new RuntimeException("Non root frame");
        }
        if (!(value instanceof Serializable)) {
            throw new RuntimeException("Non serializable value");
        }

        f.setLocal(key, (Serializable) value);
    }
}
