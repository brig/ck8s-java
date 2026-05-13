package ca.vanzyl.ck8s.aws;

import java.util.Map;

public final class TemplateUtils {

    public static String replaceTemplatesInString(String text, Map<String, String> args) {
        for (Map.Entry<String, String> arg : args.entrySet()) {
            String search = arg.getKey();
            String replace = arg.getValue();
            text = text.replace("<" + search + ">", replace);
        }

        return text;
    }

    private TemplateUtils() {
    }
}
