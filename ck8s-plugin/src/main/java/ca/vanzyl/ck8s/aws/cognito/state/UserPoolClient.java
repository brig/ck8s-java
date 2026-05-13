package ca.vanzyl.ck8s.aws.cognito.state;

import ca.vanzyl.ck8s.state.Entity;

import java.io.OutputStream;
import java.io.PrintWriter;

public record UserPoolClient(String poolId, String clientId, String clientName) implements Entity {

    @Override
    public String entityName() {
        return clientName;
    }

    @Override
    public void dump(OutputStream out) {
        PrintWriter writer = new PrintWriter(out);
        writer.println("user pool client: " + clientName());
        writer.flush();
    }
}
