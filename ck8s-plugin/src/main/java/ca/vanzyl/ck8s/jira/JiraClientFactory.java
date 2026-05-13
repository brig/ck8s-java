package ca.vanzyl.ck8s.jira;

import com.walmartlabs.concord.plugins.jira.JiraClientCfg;

public class JiraClientFactory {

    public static Ck8sJiraHttpClient create(JiraClientCfg cfg) {
        return new NativeCk8sJiraHttpClient(cfg);
    }
}
