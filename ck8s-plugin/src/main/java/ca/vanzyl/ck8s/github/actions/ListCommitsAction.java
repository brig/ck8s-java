package ca.vanzyl.ck8s.github.actions;

import ca.vanzyl.ck8s.actions.DryRunPhase;
import ca.vanzyl.ck8s.github.Ck8sGithubTaskAction;
import ca.vanzyl.ck8s.github.Ck8sGithubTaskParams;
import com.google.gson.reflect.TypeToken;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.client.PagedRequest;
import org.eclipse.egit.github.core.service.CommitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

public class ListCommitsAction extends Ck8sGithubTaskAction<Ck8sGithubTaskParams.ListCommits> {

    private final static Logger log = LoggerFactory.getLogger(ListCommitsAction.class);

    @Override
    public Action action() {
        return Action.LIST_COMMITS;
    }

    @Override
    public Set<DryRunPhase> dryRunPhases() {
        return Set.of(DryRunPhase.DRY_RUN);
    }

    @Override
    public TaskResult execute(Context context, Ck8sGithubTaskParams.ListCommits input) throws Exception {
        if (input.toSha().equals(input.fromSha())) {
            return TaskResult.success()
                    .value("messages", List.of())
                    .value("filterMatches", List.of());
        }

        var client = createClient(input);
        client.setOAuth2Token(input.baseParams().token());

        var repo = RepositoryId.create(input.org(), input.repo());

        var commitService = new CommitServiceExt(client);
        var filter = input.filter();

        try {
            var messages = new ArrayList<String>();
            var filterMatches = new ArrayList<String>();
            var count = 0;
            var stop = false;
            var startCollect = false;
            for (var commitsPage : commitService.pageCommits(repo, input.shaOrBranch(), input.since(), input.pageSize())) {
                for (RepositoryCommit commit : commitsPage) {
                    if (commit.getSha().equals(input.toSha())) {
                        startCollect = true;
                    }

                    if (commit.getSha().equals(input.fromSha()) || count >= input.max()) {
                        stop = true;
                        break;
                    }

                    if (startCollect) {
                        processCommitMessage(commit, filter, messages, filterMatches);
                    }

                    count++;
                }
                if (stop) {
                    break;
                }
            }

            log.info("filterMatches: {}", filterMatches);

            log.info("✅ Loaded {} commits in '{}/{}' since '{}', from sha '{}' to sha '{}'",
                    messages.size(), input.org(), input.repo(), input.since(), input.fromSha(), input.toSha());

            return TaskResult.success()
                    .value("messages", messages)
                    .value("filterMatches", filterMatches);
        } catch (Exception e) {
            log.error("❌ Failed to list commits '{}/{}' since '{}'", input.org(), input.repo(), input.since(), e);
            return TaskResult.fail(e);
        }
    }

    private void processCommitMessage(RepositoryCommit commit, Pattern filter, ArrayList<String> messages, ArrayList<String> filterMatches) {
        var firstLine = Optional.ofNullable(commit.getCommit())
                .map(Commit::getMessage)
                .map(msg -> msg.lines().findFirst().orElse(""))
                .orElse("");
        if (filter == null) {
            messages.add(firstLine);
            return;
        }

        var matcher = filter.matcher(firstLine);
        if (matcher.matches()) {
            messages.add(firstLine);
            if (matcher.groupCount() > 0) {
                var match = matcher.group(1);
                if (match != null) {
                    filterMatches.add(match.trim());
                }
            }
        }
    }

    private static class CommitServiceExt extends CommitService {

        public CommitServiceExt(GitHubClient client) {
            super(client);
        }

        public PageIterator<RepositoryCommit> pageCommits(IRepositoryIdProvider repository, String sha, String since, int size) {
            var id = this.getId(repository);
            var uri = new StringBuilder("/repos");
            uri.append('/').append(id);
            uri.append("/commits");
            PagedRequest<RepositoryCommit> request = this.createPagedRequest(1, size);
            request.setUri(uri);
            request.setType((new TypeToken<List<RepositoryCommit>>() {
            }).getType());

            var parameters = new HashMap<String, String>();
            if (sha != null) {
                parameters.put("sha", sha);
            }
            if (since != null) {
                parameters.put("since", since);
            }

            if(!parameters.isEmpty()) {
                request.setParams(parameters);
            }

            return this.createPageIterator(request);
        }
    }
}
