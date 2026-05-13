package ca.vanzyl.ck8s.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;

import static org.junit.Assert.assertEquals;

public class StsAssumeRoleTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testSerialization() throws Exception {
        StsAssumeRole role = StsAssumeRole.from("test-profile", Region.US_EAST_1, "test-role-arn", "test-role-session");

        String str = objectMapper.writeValueAsString(role);

        StsAssumeRole out = objectMapper.readValue(str, StsAssumeRole.class);
        assertEquals(role, out);
    }
}
