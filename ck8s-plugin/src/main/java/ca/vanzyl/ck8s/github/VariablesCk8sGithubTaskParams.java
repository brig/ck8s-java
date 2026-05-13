package ca.vanzyl.ck8s.github;

import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.UserDefinedException;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import java.util.regex.Pattern;

import static ca.vanzyl.ck8s.github.Ck8sGithubTaskParams.*;

public final class VariablesCk8sGithubTaskParams {

    public static ListCommits listCommits(Context context, Variables variables) {
        return new ListCommits(
                baseParams(context, variables),
                assertOrg(variables),
                assertRepo(variables),
                variables.assertString("sha"),
                variables.getString("since"),
                variables.assertString("fromSha"),
                variables.assertString("toSha"),
                variables.getInt("pageSize", 30),
                variables.getInt("max", 5000),
                pattern(variables, "filter")
        );
    }

    public static GetShortCommitSha getShortSha(Context context, Variables variables) {
        return new GetShortCommitSha(
                baseParams(context, variables),
                assertOrg(variables),
                assertRepo(variables),
                variables.assertString("sha"),
                variables.getInt("minLength", 7));
    }

    private static String assertOrg(Variables variables) {
        return variables.assertString("org");
    }

    private static String assertRepo(Variables variables) {
        return variables.assertString("repo");
    }

    private static BaseParams baseParams(Context context, Variables variables) {
        return new BaseParams(variables.assertString("apiUrl"), variables.assertString("token"));
    }

    private static Pattern pattern(Variables variables, String key) {
        var str = variables.getString(key);
        if (str == null) {
            return null;
        }
        try {
            return Pattern.compile(str);
        } catch (Exception e) {
            throw new UserDefinedException("Invalid '" + key + "' value: " + e.getMessage());
        }
    }
}
