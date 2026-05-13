package ca.vanzyl.ck8s.actions;

public enum DryRunPhase {
    // just dry run mode
    DRY_RUN ("dry-run"),
    // dry run mode with changes preview
    PREVIEW ("preview"),
    // dry run mode with CloudFormation Render
    AWS_PERMISSIONS("aws-permissions");

    private final String phase;

    DryRunPhase(String phase) {
        this.phase = phase;
    }

    public String getPhase() {
        return phase;
    }

    public static DryRunPhase find(String phase) {
        for (DryRunPhase ph : DryRunPhase.values()) {
            if (ph.getPhase().equals(phase)) {
                return ph;
            }
        }
        return null;
    }
}
