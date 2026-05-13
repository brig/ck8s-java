package ca.vanzyl.ck8s.aws;

import ca.vanzyl.ck8s.common.MapUtils;
import ca.vanzyl.ck8s.common.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PolicyContent {

    private final static Logger log = LoggerFactory.getLogger(PolicyContent.class);

    public static PolicyContent fromFile(Path path) {
        return new PolicyContent(Mapper.json().readMap(path));
    }

    public static PolicyContent fromString(String policyContent) throws IOException {
        return new PolicyContent(Mapper.json().readMap(policyContent));
    }

    public static String toString(PolicyContent policyContent) {
        return Mapper.json().writeAsString(policyContent.content);
    }

    public static PolicyContent fromTemplate(Path path, Map<String, String> args) throws IOException {
        PolicyContent policyContent = PolicyContent.fromFile(path);
        for (var statement : policyContent.statements()) {
            List<String> renderedResources = new ArrayList<>();
            for (String resource : statement.resources()) {
                String updatedResource = TemplateUtils.replaceTemplatesInString(resource, args);
                renderedResources.add(updatedResource);
            }
            statement.resources(renderedResources);
        }
        return policyContent;
    }

    public static boolean remove(PolicyContent policyContent, PolicyContent policyToRemove) {
        boolean policyUpdated = false;
        for (Statement statementToRemove : policyToRemove.statements()) {
            Statement statement = policyContent.assertStatement(statementToRemove.sid());
            List<String> resources = new ArrayList<>(statement.resources());
            boolean updated = resources.removeAll(statementToRemove.resources());
            if (updated) {
                policyUpdated = true;
                statement.resources(resources);

                log.info("Resources '{}' -> removed", statementToRemove.resources());
            }
        }

        PolicyContent.removeEmptyStatements(policyContent);

        return policyUpdated;
    }

    public static boolean add(PolicyContent policyContent, PolicyContent policyToAdd) {
        boolean policyUpdated = false;
        for (Statement statementToAdd : policyToAdd.statements()) {
            Statement statement = policyContent.assertStatement(statementToAdd.sid());
            List<String> resources = new ArrayList<>(statement.resources());
            for (String resourceToAdd : statementToAdd.resources()) {
                if (!resources.contains(resourceToAdd)) {
                    resources.add(resourceToAdd);
                    policyUpdated = true;

                    log.info("Resource '{}' -> added", resourceToAdd);
                }
            }
            statement.resources(resources);
        }

        return policyUpdated;
    }


    private static void removeEmptyStatements(PolicyContent policyContent) {
        var nonEmptyStatements  = policyContent.statements().stream()
                .filter(s -> !s.isEmpty())
                .toList();
        policyContent.statements(nonEmptyStatements);
    }

    private static final String STATEMENTS_KEY = "Statement";

    private final Map<String, Object> content;

    public PolicyContent(Map<String, Object> content) {
        this.content = content;
    }

    public Map<String, Object> content() {
        return content;
    }

    public List<Statement> statements() {
        return MapUtils.getList(content, STATEMENTS_KEY).stream()
                .map(Statement::new)
                .toList();
    }

    private void statements(List<Statement> statements) {
        content.put(STATEMENTS_KEY, statements.stream().map(Statement::content).toList());
    }

    public Statement assertStatement(String sid) {
        return statements().stream()
                .filter(statement -> statement.sid().equals(sid))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No statement found for sid: " + sid));
    }

    public static class Statement {

        private final Map<String, Object> content;

        private static final String RESOURCE_KEY = "Resource";

        public Statement(Map<String, Object> content) {
            this.content = content;
        }

        public Map<String, Object> content() {
            return content;
        }

        public String sid() {
            return MapUtils.assertString(content, "Sid");
        }

        public List<String> resources() {
            return getList(content, RESOURCE_KEY);
        }

        private void resources(List<String> resources) {
            content.put(RESOURCE_KEY, resources);
        }

        public boolean isEmpty() {
            return resources().isEmpty();
        }

        @SuppressWarnings("unchecked")
        private static List<String> getList(Map<String, Object> m, String key) {
            Object result = m.get(key);
            if (result == null) {
                return List.of();
            }

            if (result instanceof String) {
                return List.of((String) result);
            }

            if (result instanceof List) {
                return (List<String>) result;
            }

            throw new RuntimeException("Unknown type '" + result.getClass() + "' with value: " + result);
        }
    }
}
