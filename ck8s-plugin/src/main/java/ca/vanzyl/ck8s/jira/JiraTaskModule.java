package ca.vanzyl.ck8s.jira;

import ca.vanzyl.ck8s.jira.actions.*;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;

import javax.inject.Named;

@Named
public class JiraTaskModule implements com.google.inject.Module {

    @Override
    public void configure (Binder binder) {
        var actions = Multibinder.newSetBinder(binder, Ck8sJiraTaskAction.class);
        actions.addBinding().to(CreateVersionAction.class);
        actions.addBinding().to(GetVersionAction.class);
        actions.addBinding().to(UpdateVersionAction.class);
        actions.addBinding().to(EditIssueAction.class);
        actions.addBinding().to(GetIssueAction.class);
        actions.addBinding().to(UpsertVersionAction.class);
    }
}
