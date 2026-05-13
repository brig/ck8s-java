package ca.vanzyl.ck8s.github;

import ca.vanzyl.ck8s.MockTestContext;
import ca.vanzyl.ck8s.github.actions.ListCommitsAction;
import ca.vanzyl.ck8s.github.actions.ShortCommitShaAction;
import ca.vanzyl.ck8s.jira.actions.GetIssueAction;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Objects;
import java.util.regex.Pattern;

@Ignore
public class GithubTaskTest {

    @Test
    public void testListCommits() throws Exception{
        var apiUrl = "https://api.github.com";
        var token = Objects.requireNonNull(System.getenv("TOKEN"));

        var org = Objects.requireNonNull(System.getenv("ORG"));
        var repo = Objects.requireNonNull(System.getenv("REPO"));
        var sha = "main";
        String since = null; //"2025-05-02T23:00:00Z";
        var fromSha = "09403cbec536e58b82e2cf1a95521efc075a77ed";
        var toSha = "09403cbec536e58b82e2cf1a95521efc075a77ed";
        var pageSize = 100;
        var max = 500;
        var filter = Pattern.compile(".*(PE[ -]\\d{1,4}).*");

        var action = new ListCommitsAction();
        var result = action.execute(new MockTestContext(),
                new Ck8sGithubTaskParams.ListCommits(new Ck8sGithubTaskParams.BaseParams(apiUrl, token),
                        org, repo, sha, since, fromSha, toSha, pageSize, max, filter));
    }

    @Test
    public void getShortSha() throws Exception {
        var apiUrl = "https://api.github.com";
        var token = Objects.requireNonNull(System.getenv("TOKEN"));

        var org = Objects.requireNonNull(System.getenv("ORG"));
        var repo = Objects.requireNonNull(System.getenv("REPO"));
        var sha = "1e2a1af9d40ba68b27e241d610769493a4018686";

        var action = new ShortCommitShaAction();
        var result = action.execute(new MockTestContext(),
                new Ck8sGithubTaskParams.GetShortCommitSha(new Ck8sGithubTaskParams.BaseParams(apiUrl, token),
                        org, repo, sha, 7));

        System.out.println(result);
    }
}
