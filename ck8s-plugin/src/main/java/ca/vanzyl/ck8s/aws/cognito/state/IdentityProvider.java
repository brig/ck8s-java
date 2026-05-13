package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.state.Entity;

import java.io.OutputStream;
import java.io.PrintWriter;

public record IdentityProvider(String poolId, String name) implements Entity {

    @Override
    public String entityName() {
        return name;
    }

    @Override
    public void dump(OutputStream out) {
        PrintWriter writer = new PrintWriter(out);
        writer.println("identity provider: " + name());
        writer.flush();
    }
}
