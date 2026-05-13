package ca.vanzyl.ck8s.aws.s3;

import com.walmartlabs.concord.runtime.v2.sdk.MapBackedVariables;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class VariablesS3TaskParamsTest {

    @Test
    public void testPublicAccessBlockConfigurationDeserialization() {
        var m = new HashMap<String, Object>();
        m.put("BlockPublicAcls", true);
        m.put("IgnorePublicAcls", true);
        m.put("BlockPublicPolicy", true);
        m.put("RestrictPublicBuckets", true);

        var cfg = VariablesS3TaskParams.publicAccessBlock(new MapBackedVariables(Map.of("publicAccessBlock", m)));
        assertNotNull(cfg);
        assertTrue(cfg.blockPublicAcls());
        assertTrue(cfg.ignorePublicAcls());
        assertTrue(cfg.blockPublicPolicy());
        assertTrue(cfg.restrictPublicBuckets());
    }

    @Test
    public void testBucketConfigurationDeserialize() {
        var m = new HashMap<String, Object>();
        m.put("LocationConstraint", BucketLocationConstraint.US_EAST_2.toString());

        var cfg = VariablesS3TaskParams.configuration(new MapBackedVariables(Map.of("configuration", m)));
        assertNotNull(cfg);
        assertEquals(BucketLocationConstraint.US_EAST_2, cfg.locationConstraint());
        assertNull(cfg.bucket());
    }
}
