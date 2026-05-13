package ca.vanzyl.ck8s.utils;

import java.util.ArrayList;
import java.util.List;

public class YamlValuesCleaner {

    public static String removeEmptyValueLines(String yaml) {
        var lines = yaml.split("\n", -1);
        List<String> result = new ArrayList<>();
        processBlock(lines, 0, result, -1);

        var output = String.join("\n", result);

        if (!output.isEmpty()) {
            if (yaml.endsWith("\n") && !output.endsWith("\n")) {
                output = output + "\n";
            } else if (!yaml.endsWith("\n") && output.endsWith("\n")) {
                output = output.substring(0, output.length() - 1);
            }
        }

        return output;
    }

    private static int processBlock(String[] lines, int startIndex, List<String> output, int parentIndent) {
        var i = startIndex;

        while (i < lines.length) {
            var line = lines[i];
            var trimmed = line.trim();

            if (isCommentOrEmpty(trimmed)) {
                output.add(line);
                i++;
                continue;
            }

            var indent = getIndent(line);
            if (indent <= parentIndent) {
                break;
            }

            var decision = decideLine(lines, i, indent);

            if (decision.keep) {
                output.add(line);
                i = decision.hasChildren
                        ? processBlock(lines, i + 1, output, indent)
                        : i + 1;
            } else {
                i = decision.hasChildren
                        ? skipBlock(lines, i + 1, indent)
                        : i + 1;
            }
        }

        return i;
    }

    private static LineDecision decideLine(String[] lines, int index, int currentIndent) {
        var line = lines[index];
        var trimmed = line.trim();

        if (trimmed.startsWith("- ")) {
            return decideListItem(lines, index, currentIndent, trimmed);
        }

        return decideKeyValue(lines, index, currentIndent, trimmed);
    }

    private static LineDecision decideListItem(String[] lines, int index, int currentIndent, String trimmed) {
        var afterDash = trimmed.substring(2).trim();

        if (!afterDash.contains(":")) {
            return new LineDecision(!isEmptyLiteral(afterDash), false);
        }

        var colonIndex = afterDash.indexOf(':');
        var value = afterDash.substring(colonIndex + 1).trim();

        return decideByValue(value, lines, index, currentIndent);
    }

    private static LineDecision decideKeyValue(String[] lines, int index, int currentIndent, String trimmed) {
        var colonIndex = trimmed.indexOf(':');
        if (colonIndex < 0) {
            return new LineDecision(true, false);
        }

        var value = trimmed.substring(colonIndex + 1).trim();

        return decideByValue(value, lines, index, currentIndent);
    }

    private static LineDecision decideByValue(String value, String[] lines, int index, int currentIndent) {
        value = removeInlineComment(value);

        if (!value.isEmpty()) {
            return new LineDecision(!isEmptyLiteral(value), false);
        }

        var hasContent = hasNonEmptyChildren(lines, index + 1, currentIndent);
        return new LineDecision(hasContent, true);
    }

    private static boolean hasNonEmptyChildren(String[] lines, int startIndex, int parentIndent) {
        var i = startIndex;

        while (i < lines.length) {
            var line = lines[i];
            var trimmed = line.trim();

            if (isCommentOrEmpty(trimmed)) {
                i++;
                continue;
            }

            var indent = getIndent(line);
            if (indent <= parentIndent) {
                break;
            }

            var decision = decideLine(lines, i, indent);
            if (decision.keep) {
                return true;
            }

            i = decision.hasChildren
                    ? skipBlock(lines, i + 1, indent)
                    : i + 1;
        }

        return false;
    }

    private static int skipBlock(String[] lines, int startIndex, int parentIndent) {
        var i = startIndex;

        while (i < lines.length) {
            var line = lines[i];
            var trimmed = line.trim();

            if (isCommentOrEmpty(trimmed)) {
                if (!trimmed.isEmpty() && getIndent(line) <= parentIndent) {
                    break;
                }
                i++;
                continue;
            }

            if (getIndent(line) <= parentIndent) {
                break;
            }

            i++;
        }

        return i;
    }

    private static boolean isCommentOrEmpty(String trimmed) {
        return trimmed.isEmpty() || trimmed.startsWith("#");
    }

    private static int getIndent(String line) {
        for (var i = 0; i < line.length(); i++) {
            if (line.charAt(i) != ' ' && line.charAt(i) != '\t') {
                return i;
            }
        }
        return line.length();
    }

    private static String removeInlineComment(String value) {
        var commentIndex = value.indexOf('#');
        if (commentIndex >= 0) {
            return value.substring(0, commentIndex).trim();
        }
        return value;
    }

    private static boolean isEmptyLiteral(String value) {
        return value.isEmpty()
                || value.equals("\"\"")
                || value.equals("''")
                || value.equals("null")
                || value.equals("~")
                || value.equals("[]")
                || value.equals("{}");
    }

    private record LineDecision(boolean keep, boolean hasChildren) {}
}
