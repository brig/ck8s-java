package ca.vanzyl.ck8s.cloudformation;

import java.util.Set;

public record Statement (String effect, Set<String> actions, Set<Resource> resources) {

    public static final String ALLOW = "Allow";
}
