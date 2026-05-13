package ca.vanzyl.ck8s.aws.efs;

import ca.vanzyl.ck8s.MockTestContext;
import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import org.junit.Test;
import software.amazon.awssdk.services.efs.model.Tag;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class VariablesEfsTaskParamsTest {

    @Test
    public void testCreateAccessPointParams() {
        var m = Map.of(
                "region", "us-east-1",
                "efsId", "efs-123456",
                "name", "access-point-name",

                "rootDirectory", Map.of(
                        "Path", "/abc",
                        "CreationInfo", Map.of("OwnerUid", 1, "OwnerGid", 2, "Permissions", 755)
                ),
                "tags", Map.of("Name", "TEST")
        );

        var parsed = VariablesEfsTaskParams.createAccessPointParams(new MockTestContext(Path.of("/")), new MapBackedVariables(m));

        assertEquals("efs-123456", parsed.efsId());
        assertEquals("access-point-name", parsed.name());

        assertEquals("/abc", parsed.rootDirectory().path());
        assertEquals((Long)1L, parsed.rootDirectory().creationInfo().ownerUid());
        assertEquals((Long)2L, parsed.rootDirectory().creationInfo().ownerGid());
        assertEquals("755", parsed.rootDirectory().creationInfo().permissions());

        assertEquals(List.of(Tag.builder().key("Name").value("TEST").build()), parsed.tags());
    }
}
