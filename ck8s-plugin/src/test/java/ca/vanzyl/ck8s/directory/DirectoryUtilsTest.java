package ca.vanzyl.ck8s.directory;

import com.walmartlabs.concord.plugins.TestSupport;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static ca.vanzyl.ck8s.directory.DirectoryUtils.manifests;
import static org.assertj.core.api.Assertions.assertThat;

public class DirectoryUtilsTest
        extends TestSupport
{

    @Test
    public void validateProductionManifestScanner()
            throws Exception
    {
        delete("profiles");
        touch("profiles/kyverno/policy-one.yaml");
        touch("profiles/kyverno/policy-two.yaml");
        touch("profiles/kyverno/test/one.yaml");
        touch("profiles/kyverno/test/two.yaml");

        List<String> manifests = manifests(directory("profiles/kyverno"));
        Collections.sort(manifests);
        assertThat(manifests.get(0)).containsSequence("policy-one.yaml");
        assertThat(manifests.get(1)).containsSequence("policy-two.yaml");
    }
}
