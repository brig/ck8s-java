package ca.vanzyl.ck8s.aws.iam.state;

import ca.vanzyl.ck8s.state.Entity;

import java.io.OutputStream;
import java.io.PrintWriter;

public record IamManagedPolicy(String arn, String name, String document) implements Entity {

    @Override
    public String entityName() {
        return name();
    }

    @Override
    public void dump(OutputStream out) {
        PrintWriter writer = new PrintWriter(out);
        writer.println("arn: " + arn());
        writer.println("name: " + name());
        writer.println("document:");
        for (var line : document().split("\n")) {
            writer.println("   " + line);
        }
        writer.flush();
    }
}
