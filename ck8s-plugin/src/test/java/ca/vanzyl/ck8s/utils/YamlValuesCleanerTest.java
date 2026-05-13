package ca.vanzyl.ck8s.utils;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class YamlValuesCleanerTest {

    @Test
    public void removesEmptyLeafAndPrunesParents() {
        String input = """
                aep:
                  externalDatabase: false
                  test: "test-value"
                  cognito:
                    uiUserPoolClientName:
                """;

        String expected = """
                aep:
                  externalDatabase: false
                  test: "test-value"
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void removesInlineEmptyValues() {
        String input = """
                root:
                  empty1: ""
                  empty2: ''
                  empty3: null
                  empty4: ~
                  empty5: []
                  empty6: {}
                  keep: value
                """;

        String expected = """
                root:
                  keep: value
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void keepsCommentsWhenContentExists() {
        String input = """
                root:
                  # comment
                  keep: value
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(input);
    }

    @Test
    public void removesBlocksWithOnlyComments() {
        String input = """
                root:
                  # comment
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo("");
    }

    @Test
    public void preservesTrailingNewlineBehavior() {
        String input = "root:\n"
                + "  keep: value";

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(input);
    }

    @Test
    public void preservesBlankLinesAroundRemovedValues() {
        String input = """
                root:
                  emptyValue: null
                
                  keep: value
                """;

        String expected = """
                root:
                
                  keep: value
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void removesEmptyStringValues() {
        String input = """
                key1: ""
                key2: value
                key3: ''
                """;

        String expected = """
                key2: value
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void removesNullValues() {
        String input = """
                key1: null
                key2: value
                key3: ~
                """;

        String expected = """
                key2: value
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void removesEmptyArraysAndObjects() {
        String input = """
                key1: []
                key2: value
                key3: {}
                """;

        String expected = """
                key2: value
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void removesEmptyNestedBlocks() {
        String input = """
                root:
                  empty1: ""
                  empty2: null
                  nested:
                    alsoEmpty: []
                """;

        String expected = """
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void keepsNestedBlocksWithContent() {
        String input = """
                root:
                  nested:
                    keep: value
                """;

        String expected = """
                root:
                  nested:
                    keep: value
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void removesEmptyNestedBlocksButKeepsSiblings() {
        String input = """
                root:
                  emptyChild:
                    emptyValue: ""
                  sibling:
                    keep: value
                """;

        String expected = """
                root:
                  sibling:
                    keep: value
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void removesCommentsBeforeEmptyBlocks() {
        String input = """
            root:
              # This comment should be removed
              emptyBlock:
                empty: ""
              # This comment should stay
              kept: value
            """;

        String expected = """
            root:
              # This comment should be removed
              # This comment should stay
              kept: value
            """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void preservesComments() {
        String input = """
            # Top comment
            root:
              # Comment before kept value
              keep: value
              # Comment before empty value
              empty: ""
            """;

        String expected = """
            # Top comment
            root:
              # Comment before kept value
              keep: value
              # Comment before empty value
            """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void handlesInlineComments() {
        String input = """
                key1: "" # inline comment
                key2: value # keep this
                key3: null # remove this
                """;

        String expected = """
                key2: value # keep this
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void removesEmptyListItems() {
        String input = """
                list:
                  - ""
                  - value
                  - null
                  - another
                """;

        String expected = """
                list:
                  - value
                  - another
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void removesEmptyListItemsWithKeys() {
        String input = """
                items:
                  - name: ""
                  - name: valid
                  - value: null
                  - value: 123
                """;

        String expected = """
                items:
                  - name: valid
                  - value: 123
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void keepsListItemsWithNestedContent() {
        String input = """
                items:
                  - name:
                      first: John
                      last: Doe
                  - value:
                      empty: ""
                """;

        String expected = """
                items:
                  - name:
                      first: John
                      last: Doe
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void preservesBlankLines() {
        String input = """
                key1: value1
                
                key2: value2
                key3: ""
                
                key4: value4
                """;

        String expected = """
                key1: value1
                
                key2: value2
                
                key4: value4
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void handlesComplexNestedStructure() {
        String input = """
                app:
                  database:
                    host: localhost
                    port: ""
                    credentials:
                      user: admin
                      password: ""
                  cache:
                    enabled: null
                  server:
                    port: 8080
                """;

        String expected = """
                app:
                  database:
                    host: localhost
                    credentials:
                      user: admin
                  server:
                    port: 8080
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void handlesEmptyInput() {
        String input = "";
        String expected = "";
        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void handlesOnlyComments() {
        String input = """
                # Comment 1
                # Comment 2
                """;

        String expected = """
                # Comment 1
                # Comment 2
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void handlesOnlyEmptyValues() {
        String input = """
                key1: ""
                key2: null
                key3: []
                """;

        String expected = """
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void preservesTrailingNewline() {
        String input = "key: value\n";
        String expected = "key: value\n";
        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void removesTrailingNewlineWhenInputDoesntHaveIt() {
        String input = "key: value";
        String expected = "key: value";
        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void handlesMultiLevelNesting() {
        String input = """
                level1:
                  level2:
                    level3:
                      level4:
                        empty: ""
                      kept: value
                """;

        String expected = """
                level1:
                  level2:
                    level3:
                      kept: value
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void handlesMixedListsAndObjects() {
        String input = """
                config:
                  items:
                    - name: item1
                      value: ""
                    - name: item2
                      value: 100
                  settings:
                    timeout: null
                    retries: 3
                """;

        String expected = """
                config:
                  items:
                    - name: item1
                    - name: item2
                      value: 100
                  settings:
                    retries: 3
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void keepsNumericZeroValues() {
        String input = """
                key1: 0
                key2: ""
                key3: 0.0
                """;

        String expected = """
                key1: 0
                key3: 0.0
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }

    @Test
    public void keepsBooleanFalseValues() {
        String input = """
                key1: false
                key2: ""
                key3: true
                """;

        String expected = """
                key1: false
                key3: true
                """;

        assertThat(YamlValuesCleaner.removeEmptyValueLines(input)).isEqualTo(expected);
    }
}
