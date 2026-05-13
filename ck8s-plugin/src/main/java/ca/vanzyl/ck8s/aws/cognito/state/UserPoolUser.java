package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.state.Entity;

import java.io.OutputStream;
import java.io.PrintWriter;

public record UserPoolUser(String poolId, String username) implements Entity {

    @Override
    public String entityName() {
        return username;
    }

    @Override
    public void dump(OutputStream out) {
        PrintWriter writer = new PrintWriter(out);
        writer.println("user pool user: " + username());
        writer.flush();
    }
}
