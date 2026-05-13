package ca.vanzyl.ck8s.aws.iam.state;

import ca.vanzyl.ck8s.state.Entity;

import java.io.OutputStream;
import java.io.PrintWriter;

public record IamInlinePolicy(String roleName, String name, String document) implements Entity {

    @Override
    public String entityName() {
        return roleName + ':' + name;
    }

    @Override
    public void dump(OutputStream out) {
        PrintWriter writer = new PrintWriter(out);
        writer.println("roleName: " + roleName());
        writer.println("name: " + name());
        writer.println("document:");
        for (var line : document().split("\n")) {
            writer.println("   " + line);
        }
        writer.flush();
    }
}
