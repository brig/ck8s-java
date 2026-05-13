package ca.vanzyl.ck8s;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ExpressionExtractorTest {

    @Test
    public void testSimple() {
        String input = "${concord:var}";
        List<String> expressions = ExpressionExtractor.collectExpressions(input);
        assertEquals(1, expressions.size());
        assertEquals("${concord:var}", expressions.get(0));
    }

    @Test
    public void testNoExpression() {
        String input = "No expressions here";
        List<String> expressions = ExpressionExtractor.collectExpressions(input);
        assertEquals(0, expressions.size());
    }

    @Test
    public void testInner1() {
        String input = "\"${concord:outer {inner}}";
        List<String> expressions = ExpressionExtractor.collectExpressions(input);
        assertEquals(1, expressions.size());
        assertEquals("${concord:outer {inner}}", expressions.get(0));
    }

    @Test
    public void testInner2() {
        String input = "${concord:nested {expression ${inside} of another}}";
        List<String> expressions = ExpressionExtractor.collectExpressions(input);
        assertEquals(1, expressions.size());
        assertEquals("${concord:nested {expression ${inside} of another}}", expressions.get(0));

        String input2 = "${concord:nested {expression ${concord:inside} of another}}";
        List<String> expressions2 = ExpressionExtractor.collectExpressions(input2);
        assertEquals(1, expressions2.size());
        assertEquals("${concord:nested {expression ${concord:inside} of another}}", expressions2.get(0));
    }

    @Test
    public void testMulti() {
        String input5 = "${concord:first} some text ${concord:second} and ${concord:third {curly braces} inside}";
        List<String> expressions5 = ExpressionExtractor.collectExpressions(input5);
        assertEquals(3, expressions5.size());
        assertEquals("${concord:first}", expressions5.get(0));
        assertEquals("${concord:second}", expressions5.get(1));
        assertEquals("${concord:third {curly braces} inside}", expressions5.get(2));

        String input6 = "${first} some text ${second} and $\n{third \n{curly braces} inside}";
        List<String> expressions6 = ExpressionExtractor.collectExpressions(input6);
        assertEquals(0, expressions6.size());

        String input7 = "${first} some text ${second} and $\n{concord:third \n{curly braces} inside}";
        List<String> expressions7 = ExpressionExtractor.collectExpressions(input7);
        assertEquals(0, expressions7.size());

        String input8 = "${first} some text ${second} and ${concord:third \n{curly braces} inside}";
        List<String> expressions8 = ExpressionExtractor.collectExpressions(input8);
        assertEquals(1, expressions8.size());
        assertEquals("${concord:third \n{curly braces} inside}", expressions8.get(0));
    }

    @Test
    public void testExpressionInsideExpression() {
        String input = "abc ${first ${concord:inside}} end";
        List<String> expressions = ExpressionExtractor.collectExpressions(input);
        assertEquals(1, expressions.size());
        assertEquals("${concord:inside}", expressions.get(0));
    }

    @Test
    public void testEmptyInput() {
        String input = "";
        List<String> expressions = ExpressionExtractor.collectExpressions(input);
        assertEquals(0, expressions.size());
    }

    @Test
    public void test1() {
        String input = "$";
        List<String> expressions = ExpressionExtractor.collectExpressions(input);
        assertEquals(0, expressions.size());
    }

    @Test
    public void test2() {
        String input = "${";
        List<String> expressions = ExpressionExtractor.collectExpressions(input);
        assertEquals(0, expressions.size());
    }

    @Test
    public void test3() {
        String input = "${concord: ${concord:xyz}";
        List<String> expressions = ExpressionExtractor.collectExpressions(input);
        assertEquals(0, expressions.size());
    }

    @Test
    public void test4() {
        String input = "${{{{";
        List<String> expressions = ExpressionExtractor.collectExpressions(input);
        assertEquals(0, expressions.size());
    }

    @Test
    public void testConcordExpressionAtStart() {
        String input = "${concord:var} and some text.";
        List<String> expressions = ExpressionExtractor.collectExpressions(input);

        assertEquals(1, expressions.size());
        assertEquals("${concord:var}", expressions.get(0));
    }

    @Test
    public void testConcordExpressionAtEnd() {
        String input = "Text and an expression: ${concord:var}";
        List<String> expressions = ExpressionExtractor.collectExpressions(input);

        assertEquals(1, expressions.size());
        assertEquals("${concord:var}", expressions.get(0));
    }

    // we do not care about escape
    @Test
    public void testExpressionWithEscapedBraces() {
        String input = "Escaped braces: ${concord:expression \\{with \\}escaped\\} braces}";
        List<String> expressions = ExpressionExtractor.collectExpressions(input);

        assertEquals(1, expressions.size());
        assertEquals("${concord:expression \\{with \\}escaped\\}", expressions.get(0));
    }

    @Test
    public void testExpressionWithMultiline() {
        String input = "Multiline expression: ${concord:expression \nwith\n newlines}";
        List<String> expressions = ExpressionExtractor.collectExpressions(input);

        assertEquals(1, expressions.size());
        assertEquals("${concord:expression \nwith\n newlines}", expressions.get(0));
    }

    @Test
    public void testExpressionWithWhitespace() {
        String input = "Whitespace expression: ${concord:   expression with whitespace   }";
        List<String> expressions = ExpressionExtractor.collectExpressions(input);

        assertEquals(1, expressions.size());
        assertEquals("${concord:   expression with whitespace   }", expressions.get(0));
    }

    @Test
    public void testExpressionWithOnlyClosingBrace() {
        String input = "Expression with only closing brace: ${concord:}";
        List<String> expressions = ExpressionExtractor.collectExpressions(input);

        assertEquals(1, expressions.size());
        assertEquals("${concord:}", expressions.get(0));
    }

    @Test
    public void testExpressionWithConcordAndOtherPrefix() {
        String input = "Expression with ${concord:var} and ${other:var2}.";
        List<String> expressions = ExpressionExtractor.collectExpressions(input);

        assertEquals(1, expressions.size());
        assertEquals("${concord:var}", expressions.get(0));
    }
}
