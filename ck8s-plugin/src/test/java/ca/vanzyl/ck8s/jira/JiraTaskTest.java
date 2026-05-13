package ca.vanzyl.ck8s.jira;

import ca.vanzyl.ck8s.MockTestContext;
import ca.vanzyl.ck8s.jira.actions.*;
import com.walmartlabs.concord.plugins.jira.JiraClientCfg;
import com.walmartlabs.concord.plugins.jira.JiraCredentials;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Ignore
public class JiraTaskTest {

    private Ck8sJiraTaskParams.BaseParams baseParams;

    @Before
    public void setup() {
        var baseUrl = Objects.requireNonNull(System.getenv("BASE_URL"));
        var user = Objects.requireNonNull(System.getenv("USER"));
        var password = Objects.requireNonNull(System.getenv("PASSWORD"));

        this.baseParams = new Ck8sJiraTaskParams.BaseParams(new JiraClientCfg() {}, new JiraCredentials(user, password), baseUrl);
    }

    @Test
    public void testCreateVersion() throws Exception {
        var action = new CreateVersionAction();
        var result = action.execute(new MockTestContext(),
                new Ck8sJiraTaskParams.CreateVersionParams(baseParams, 11102, "test-v4", null, null, false, false));
    }

    @Test
    public void testUpsertVersion() throws Exception {
        var action = new UpsertVersionAction();
        var result = action.execute(new MockTestContext(),
                new Ck8sJiraTaskParams.CreateVersionParams(baseParams, 11102, "test-v4", null, null, false, false));
    }

    @Test
    public void testEditIssue() throws Exception {
        var update = Map.<String, Object>of("fixVersions", List.of(Map.of("add", Map.of("name", "RR5.34.5"))));

        var action = new EditIssueAction();
        var result = action.execute(new MockTestContext(),
                new Ck8sJiraTaskParams.EditIssueParams(baseParams, "PE-54841", null, update));
    }

    @Test
    public void testGetIssue() throws Exception {
        var action = new GetIssueAction();
        var result = action.execute(new MockTestContext(),
                new Ck8sJiraTaskParams.GetIssueParams(baseParams, "PE-54841"));
    }

    @Test
    public void testGetVersion() throws Exception {
        var action = new GetVersionAction();
        var result = action.execute(new MockTestContext(),
                new Ck8sJiraTaskParams.GetVersionParams(baseParams, 11079, "test-v4"));
    }

    @Test
    public void testUpdateVersion() throws Exception {
        var releaseDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        var action = new UpdateVersionAction();
        var result = action.execute(new MockTestContext(),
                new Ck8sJiraTaskParams.CreateVersionParams(baseParams, 11079, "test-v4", "test ME", releaseDate, true, false));

    }
}
