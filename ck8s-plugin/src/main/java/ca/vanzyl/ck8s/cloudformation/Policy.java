package ca.vanzyl.ck8s.cloudformation;

import java.util.List;

public record Policy (String name, List<Statement> statements) {
}
