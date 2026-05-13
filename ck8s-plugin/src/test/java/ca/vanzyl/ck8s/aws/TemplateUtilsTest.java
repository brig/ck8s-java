package ca.vanzyl.ck8s.aws;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TemplateUtilsTest {

    @Test
    public void testReplaceSingleTemplate() {
        String text = "Hello, <name>!";
        Map<String, String> args = Map.of("name", "John");

        String result = TemplateUtils.replaceTemplatesInString(text, args);

        assertThat(result).isEqualTo("Hello, John!");
    }

    @Test
    public void testReplaceMultipleTemplates() {
        String text = "Hello, <name>! Welcome to <place>.";
        Map<String, String> args = Map.of(
                "name", "Alice",
                "place", "Wonderland");

        String result = TemplateUtils.replaceTemplatesInString(text, args);

        assertThat(result).isEqualTo("Hello, Alice! Welcome to Wonderland.");
    }

    @Test
    public void testReplaceWithMissingTemplate() {
        String text = "Hello, <name>! Welcome to <place>.";
        Map<String, String> args = Map.of("name", "Bob");

        String result = TemplateUtils.replaceTemplatesInString(text, args);

        assertThat(result).isEqualTo("Hello, Bob! Welcome to <place>.");
    }

    @Test
    public void testNoTemplatesInText() {
        String text = "Hello, World!";
        Map<String, String> args = Map.of("name", "John");

        String result = TemplateUtils.replaceTemplatesInString(text, args);

        assertThat(result).isEqualTo("Hello, World!");
    }

    @Test
    public void testEmptyArgs() {
        String text = "Hello, <name>!";
        Map<String, String> args = new HashMap<>();

        String result = TemplateUtils.replaceTemplatesInString(text, args);

        assertThat(result).isEqualTo("Hello, <name>!");
    }
}
