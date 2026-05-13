package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.state.Entity;

import java.io.OutputStream;
import java.io.PrintWriter;

public record ResourceServer(String poolId, String identifier, String name) implements Entity {

    @Override
    public String entityName() {
        return identifier;
    }

    @Override
    public void dump(OutputStream out) {
        PrintWriter writer = new PrintWriter(out);
        writer.println("resource server: " + identifier());
        writer.flush();
    }
}
