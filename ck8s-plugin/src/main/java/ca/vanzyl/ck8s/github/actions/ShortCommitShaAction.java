package ca.vanzyl.ck8s.github.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.github.Ck8sGithubTaskAction;
import ca.vanzyl.ck8s.github.Ck8sGithubTaskParams;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.CommitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

public class ShortCommitShaAction extends Ck8sGithubTaskAction<Ck8sGithubTaskParams.GetShortCommitSha> {

    private final static Logger log = LoggerFactory.getLogger(ShortCommitShaAction.class);

    @Override
    public Action action() {
        return Action.GET_SHORT_SHA;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, Ck8sGithubTaskParams.GetShortCommitSha input) throws Exception {
        int minLen = input.minLength() > 0 ? input.minLength() : 7;

        var client = createClient(input);
        client.setOAuth2Token(input.baseParams().token());

        var repo = RepositoryId.create(input.org(), input.repo());
        var commitService = new CommitService(client);

        var exists = isCommitExists(commitService, repo, input.sha());
        if (!exists) {
            log.error("❌ Commit '{}' not found in '{}/{}'", input.sha(), input.org(), input.repo());
            return TaskResult.fail("Commit not found in '" + input.org() + "/" + input.repo() + "'");
        }

        for (int len = minLen; len <= 40; len++) {
            var prefix = input.sha().substring(0, len);

            try {
                var commit = commitService.getCommit(repo, prefix);
                if (commit.getSha().equals(input.sha())) {

                    log.info("✅ Short SHA for '{}' commit is '{}' in '{}/{}'",
                            input.sha(), prefix, input.org(), input.repo());

                    return TaskResult.success()
                            .value("shortSha", prefix);
                }
            } catch (RequestException e) {
                if (e.getStatus() == 404 || e.getStatus() == 422) {
                    continue;
                }

                log.error("❌ Error while getting short SHA for '{}' commit in '{}/{}'",
                        input.sha(), input.org(), input.repo(), e);

                return TaskResult.fail("Failed to get short SHA for '" + input.sha() + "' commit");
            }
        }

        log.error("❌ Could not derive unique short SHA for '{}' commit in '{}/{}'",
                input.sha(), input.org(), input.repo());

        return TaskResult.fail("Could not derive unique short SHA for '" + input.sha() + "' commit");
    }

    private static boolean isCommitExists(CommitService commitService, RepositoryId repo, String sha) throws IOException {
        try {
            var c = commitService.getCommit(repo, sha);
            return c != null && sha.equalsIgnoreCase(c.getSha());
        } catch (RequestException e) {
            if (e.getStatus() == 404) {
                return false;
            }
            throw e;
        }
    }
}
