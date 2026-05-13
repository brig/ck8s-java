package ca.vanzyl.ck8s.aws.cloudformation.state;

import ca.vanzyl.ck8s.state.Entity;
import org.immutables.value.Value;

import java.io.OutputStream;
import java.io.PrintWriter;

@Value.Style(jdkOnly = true)
@Value.Immutable
public interface CloudFormationEntity extends Entity {

    String stackName();

    @Override
    default String entityName() {
        return stackName();
    }

    @Override
    default void dump(OutputStream out) {
        PrintWriter writer = new PrintWriter(out);
        writer.println("name: " + stackName());
    }

    static ImmutableCloudFormationEntity.Builder builder() {
        return ImmutableCloudFormationEntity.builder();
    }
}
