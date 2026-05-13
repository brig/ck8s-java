package ca.vanzyl.ck8s.aws.iam.state;

import ca.vanzyl.ck8s.state.Entity;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Value.Style(jdkOnly = true)
@Value.Immutable
public interface IamRole extends Entity {

    String roleName();

    @Nullable
    String trustPolicy();

    @Value.Default
    default Map<String, String> tags() {
        return Map.of();
    }

    @Value.Default
    default Set<String> inlinePolicyNames() {
        return Set.of();
    }

    @Value.Default
    default Set<String> attachedPolicyArns() {
        return Set.of();
    }

    static ImmutableIamRole.Builder builder() {
        return ImmutableIamRole.builder();
    }

    @Override
    default String entityName() {
        return roleName();
    }

    @Override
    default void dump(OutputStream out) {
        PrintWriter writer = new PrintWriter(out);
        writer.println("role: " + roleName());
        if (trustPolicy() != null) {
            writer.println("trustPolicy:");
            for (var line : Objects.requireNonNull(trustPolicy()).split("\n")) {
                writer.println("   " + line);
            }
        }
        if (tags().isEmpty()) {
            writer.println("tags:");
            tags().forEach((k, v) -> writer.append("  ").append(k).append(": ").append(v).append("\n"));
        }
        if (!inlinePolicyNames().isEmpty()) {
            writer.println("inlinePolicies:");
            inlinePolicyNames().forEach(p -> writer.append("  - ").append(p).append("\n"));
        }
        if (!attachedPolicyArns().isEmpty()) {
            writer.println("attachedPolicies:");
            attachedPolicyArns().forEach(arn -> writer.append("  - ").append(arn).append("\n"));
        }
        writer.flush();
    }
}
