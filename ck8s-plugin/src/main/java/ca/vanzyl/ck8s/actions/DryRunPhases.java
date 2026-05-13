package ca.vanzyl.ck8s.actions;

import ca.vanzyl.ck8s.common.MapUtils;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.ProcessConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DryRunPhases {

    private static final Logger log = LoggerFactory.getLogger(DryRunPhases.class);

    // TODO: load from root frame (ignore override)
    public static DryRunPhase assertPhase(Context context) {
        var phase = context.variables().getString("phase", null);
        if (phase == null) {
            return DryRunPhase.DRY_RUN;
        }

        var p = DryRunPhase.find(phase);
        if (p != null) {
            return p;
        }

        log.error("Dry run phase '{}' is not supported. Defaulting to {}", phase, DryRunPhase.DRY_RUN);
        return DryRunPhase.DRY_RUN;
    }

    public static boolean isPreview(ProcessConfiguration processConfiguration) {
        if (!processConfiguration.dryRun()) {
            return false;
        }

        var phase = MapUtils.getString(processConfiguration.arguments(), "phase");
        return DryRunPhase.PREVIEW.getPhase().equals(phase);
    }

    private DryRunPhases() {
    }
}
