package ca.vanzyl.concord.k8s;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class K8sConverter
{

    public static String indent(String value, int indent)
    {
        String indentString = indentString(indent);
        return indentString + value.replaceAll("\n", "\n" + indentString);
    }

    public static String nindent(String variable, int indent)
    {
        return "\n" + indent(variable, indent);
    }

    private static String indentString(int indent)
    {
        return new String(new char[indent]).replace('\0', ' ');
    }

    public static String propertiesToYaml(Map<String, Map<String, String>> properties, boolean trimNewines, int indent)
    {
        if (properties == null || properties.isEmpty()) {
            return "{}";
        }

        Map<String, String> expandedProperties = properties.entrySet()
                .stream()
                .collect(toMap(e -> e.getKey(), e -> {
                    StringBuilder sb = new StringBuilder();
                    e.getValue().forEach((innerK, innerV) -> sb.append(innerK).append("=").append(innerV).append("\n"));
                    return sb.toString();
                }));
        return textFilesToYaml(expandedProperties, trimNewines, indent);
    }

    public static String textFilesToYaml(Map<String, String> files, boolean trimNewines, int indent)
    {
        if (files == null || files.isEmpty()) {
            return "{}";
        }

        String multilineStringOperator = trimNewines ? " |-\n" : " |\n";

        StringBuilder sb = new StringBuilder();
        files.forEach((k, v) -> {
            sb.append(k).append(":").append(multilineStringOperator);
            sb.append(indent(v.trim(), 2));
            sb.append("\n");
        });

        return nindent(sb.toString(), indent);
    }
}
