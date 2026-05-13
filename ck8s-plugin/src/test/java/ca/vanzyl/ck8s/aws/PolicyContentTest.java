package ca.vanzyl.ck8s.aws;

import org.junit.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyContentTest {

    @Test
    public void testReadFromFile() throws Exception {
        PolicyContent policyContent = PolicyContent.fromFile(assertResource("policy-template.json"));
        assertThat(policyContent).isNotNull();
        assertThat(policyContent.statements()).hasSize(1);

        PolicyContent.Statement statement = policyContent.statements().get(0);
        assertThat(statement.sid()).isEqualTo("DataQueriesRWAccess");
        assertThat(statement.resources()).hasSize(1);
        assertThat(statement.resources().get(0)).isEqualTo("arn:aws:s3:::<S3_BUCKET>/<S3_FOLDER>/dataqueries/*");
    }

    @Test
    public void testReadFromTemplate() throws Exception {
        Map<String, String> args = Map.of("S3_BUCKET", "test-bucket", "S3_FOLDER", "test-folder");

        PolicyContent policyContent = PolicyContent.fromTemplate(assertResource("policy-template.json"), args);
        assertThat(policyContent.statements().get(0).resources().get(0)).isEqualTo("arn:aws:s3:::test-bucket/test-folder/dataqueries/*");
    }

    @Test
    public void testRemoveResources_emptyStatementsAsResult() throws Exception {
        Map<String, String> args = Map.of("S3_BUCKET", "test-bucket", "S3_FOLDER", "test-folder");

        PolicyContent policyContent = PolicyContent.fromFile(assertResource("policy.json"));
        PolicyContent policyContentToRemove = PolicyContent.fromTemplate(assertResource("policy-template.json"), args);

        boolean updated = PolicyContent.remove(policyContent, policyContentToRemove);

        assertThat(updated).isTrue();
        assertThat(policyContent.statements().isEmpty()).isEqualTo(true);
    }

    @Test
    public void testRemoveResources_NotUpdated() throws Exception {
        Map<String, String> args = Map.of("S3_BUCKET", "test-bucket2", "S3_FOLDER", "test-folder");

        PolicyContent policyContent = PolicyContent.fromFile(assertResource("policy.json"));
        PolicyContent policyContentToRemove = PolicyContent.fromTemplate(assertResource("policy-template.json"), args);

        boolean updated = PolicyContent.remove(policyContent, policyContentToRemove);

        assertThat(updated).isFalse();
        assertThat(policyContent.statements().size()).isEqualTo(1);
        assertThat(policyContent.statements().get(0).resources().size()).isEqualTo(1);
    }

    @Test
    public void testAddResources_Updated() throws Exception {
        Map<String, String> args = Map.of("S3_BUCKET", "test-bucket2", "S3_FOLDER", "test-folder");

        PolicyContent policyContent = PolicyContent.fromFile(assertResource("policy.json"));
        PolicyContent policyContentToAdd = PolicyContent.fromTemplate(assertResource("policy-template.json"), args);

        boolean updated = PolicyContent.add(policyContent, policyContentToAdd);

        assertThat(updated).isTrue();
        assertThat(policyContent.statements().size()).isEqualTo(1);
        assertThat(policyContent.statements().get(0).resources().size()).isEqualTo(2);
        assertThat(policyContent.statements().get(0).resources()).isEqualTo(List.of("arn:aws:s3:::test-bucket/test-folder/dataqueries/*", "arn:aws:s3:::test-bucket2/test-folder/dataqueries/*"));
    }

    @Test
    public void testAddResources_NotUpdated() throws Exception {
        Map<String, String> args = Map.of("S3_BUCKET", "test-bucket", "S3_FOLDER", "test-folder");

        PolicyContent policyContent = PolicyContent.fromFile(assertResource("policy.json"));
        PolicyContent policyContentToAdd = PolicyContent.fromTemplate(assertResource("policy-template.json"), args);

        boolean updated = PolicyContent.add(policyContent, policyContentToAdd);

        assertThat(updated).isFalse();
        assertThat(policyContent.statements().size()).isEqualTo(1);
        assertThat(policyContent.statements().get(0).resources().size()).isEqualTo(1);
        assertThat(policyContent.statements().get(0).resources()).isEqualTo(List.of("arn:aws:s3:::test-bucket/test-folder/dataqueries/*"));
    }

    private static Path assertResource(String resource) throws URISyntaxException {
        var r = PolicyContentTest.class.getResource(resource);
        if (r == null) {
            throw new RuntimeException("Resource not found: " + resource);
        }

        return Path.of(r.toURI());
    }
}
