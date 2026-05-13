package ca.vanzyl.ck8s.github;

import ca.vanzyl.ck8s.github.actions.ListCommitsAction;
import ca.vanzyl.ck8s.github.actions.ShortCommitShaAction;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;

import javax.inject.Named;

@Named
public class GithubTaskModule implements com.google.inject.Module {

    @Override
    public void configure (Binder binder) {
        var actions = Multibinder.newSetBinder(binder, Ck8sGithubTaskAction.class);
        actions.addBinding().to(ListCommitsAction.class);
        actions.addBinding().to(ShortCommitShaAction.class);
    }
}
