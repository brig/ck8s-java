package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.state.Entity;

import java.io.OutputStream;
import java.io.PrintWriter;

public record UserPool(String id, String name) implements Entity {

    @Override
    public String entityName() {
        return name;
    }

    @Override
    public void dump(OutputStream out) {
        PrintWriter writer = new PrintWriter(out);
        writer.println("user pool: " + name());
        writer.flush();
    }
}
