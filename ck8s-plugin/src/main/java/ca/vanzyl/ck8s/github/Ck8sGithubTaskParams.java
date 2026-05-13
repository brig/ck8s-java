package ca.vanzyl.ck8s.github;

import ca.vanzyl.ck8s.actions.ActionInput;

import java.util.regex.Pattern;

public interface Ck8sGithubTaskParams extends ActionInput {

    BaseParams baseParams();

    record BaseParams(String apiUrl, String token) {
    }

    record ListCommits(
            Ck8sGithubTaskParams.BaseParams baseParams,
            String org,
            String repo,
            String shaOrBranch,
            String since,
            String fromSha,
            String toSha,
            int pageSize,
            int max,
            Pattern filter
    ) implements Ck8sGithubTaskParams {
    }

    record GetShortCommitSha(
            Ck8sGithubTaskParams.BaseParams baseParams,
            String org,
            String repo,
            String sha,
            int minLength
    ) implements Ck8sGithubTaskParams {
    }
}
