package ca.vanzyl.ck8s.aws.iam.state;

import ca.vanzyl.ck8s.preview.Change;
import org.junit.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class IamRoleChangesProducerTest {

    private final IamRoleChangesProducer producer = new IamRoleChangesProducer();

    @Test
    public void deleteRoleAlsoReportsChildren() {
        var key = new IamRoleKey("my-role");
        var prev = IamRole.builder()
                .roleName("my-role")
                .trustPolicy("{\"Version\":\"2012-10-17\"}")
                .tags(Map.of("owner", "fuji"))
                .inlinePolicyNames(Set.of("my-role-policy"))
                .attachedPolicyArns(Set.of("arn:aws:iam::123456789012:policy/my-managed"))
                .build();

        var changes = producer.produce(key, prev, null, Instant.now());

        assertThat(changes).allMatch(c -> c.action() == Change.Action.DELETE);

        var roleId = IamChangeType.roleId("my-role");
        assertThat(changes).filteredOn(c -> c.type().equals(IamChangeType.ROLE_TYPE))
                .singleElement()
                .satisfies(c -> assertThat(c.id()).isEqualTo(roleId));

        assertThat(changes).filteredOn(c -> !c.type().equals(IamChangeType.ROLE_TYPE))
                .extracting(Change::type)
                .containsExactlyInAnyOrder(
                        IamChangeType.TRUST_POLICY_TYPE,
                        IamChangeType.ROLE_TAG_TYPE,
                        IamChangeType.INLINE_POLICY_TYPE,
                        IamChangeType.MANAGED_POLICY_ATTACH_TYPE);

        assertThat(changes).filteredOn(c -> !c.type().equals(IamChangeType.ROLE_TYPE))
                .allSatisfy(c -> assertThat(c.parentId()).isEqualTo(roleId));
    }

    @Test
    public void deleteRoleWithoutChildrenReportsOnlyRole() {
        var prev = IamRole.builder().roleName("bare-role").build();

        var changes = producer.produce(new IamRoleKey("bare-role"), prev, null, Instant.now());

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).type()).isEqualTo(IamChangeType.ROLE_TYPE);
    }
}
