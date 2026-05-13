package ca.vanzyl.ck8s.workspace;

import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Task;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Path;

@Named("workspaceTree")
public class WorkspaceTreeViewTask
        implements Task
{

    private final Path workDir;

    @Inject
    public WorkspaceTreeViewTask(Context context)
    {
        this.workDir = context.workingDirectory();
    }

    public void render()
    {
        WorkspaceTreeView workspaceTreeView = new WorkspaceTreeView();
        workspaceTreeView.print(workDir.toFile());
    }
}
