package ca.vanzyl.ck8s.utils;

import java.util.Collection;
import java.util.Map;

public class YamlUtils
{

    public static String nindentYaml(Map<String, String> properties, int indent)
    {
        return "\n" + indentYaml(properties, indent);
    }

    public static String indentYaml(Map<String, String> properties, int indent)
    {
        if (properties == null) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        properties.forEach((k, v) -> sb.append(k).append(": ").append("\"").append(v).append("\"").append("\n"));
        sb.setLength(sb.length() - 1);
        return indent(sb.toString(), indent);
    }

    public static String indentYaml(Collection<?> properties, int indent) {
        if (properties == null || properties.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        properties.forEach((v) -> sb.append("- ").append("\"").append(v).append("\"").append("\n"));
        sb.setLength(sb.length() - 1);
        return indent(sb.toString(), indent);
    }

    public static String indent(String value, int indent)
    {
        String indentString = indentString(indent);
        return indentString + value.replaceAll("\n", "\n" + indentString);
    }

    private static String indentString(int indent)
    {
        return new String(new char[indent]).replace('\0', ' ');
    }

    public static String toCsv(Map<String, String> properties)
    {
        if (properties == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        properties.forEach((k, v) -> sb.append(k).append("=").append(v).append(","));
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
