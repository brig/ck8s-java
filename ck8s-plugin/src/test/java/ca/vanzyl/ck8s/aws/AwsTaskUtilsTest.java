package ca.vanzyl.ck8s.aws;

import ca.vanzyl.ck8s.common.Mapper;
import org.junit.Test;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.RequestLaunchTemplateData;
import software.amazon.awssdk.services.eks.model.Cluster;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AwsTaskUtilsTest {

    @Test
    public void testDeserialize() {
        Map<String, Object> data = Mapper.json().readMap(AwsTaskUtilsTest.class.getResource("config.json"));
        var r = AwsTaskUtils.deserialize(data, RequestLaunchTemplateData.serializableBuilderClass()).build();

        assertEquals("ami-latest", r.imageId());
        assertEquals(InstanceType.M5_2_XLARGE, r.instanceType());
        assertEquals(List.of("1", "2", "3"), r.securityGroupIds());
        assertEquals(2, r.tagSpecifications().size());
        assertEquals(1, r.blockDeviceMappings().size());
    }

    @Test
    public void testSerialize() {
        Cluster cluster = Cluster.builder()
                .name("test")
                .build();

        Map<String, Object> data = AwsTaskUtils.serialize(cluster);
        assertEquals("test", data.get("name"));
    }

    @Test
    public void testSerializeNull() {
        Map<String, Object> data = AwsTaskUtils.serialize(null);
        assertNull(data);
    }

    @Test
    public void testSerializeCredentials() throws Exception {
        Map<String, Object> data = AwsTaskUtils.serialize(Credentials.builder()
                .accessKeyId("test-access-key-id")
                .secretAccessKey("test-secret-access-key")
                .sessionToken("test-session-token")
                .expiration(Instant.now())
                .build());

        assertEquals("test-access-key-id", data.get("accessKeyId"));
    }
}
