package ca.vanzyl.ck8s;

import java.util.ArrayList;
import java.util.List;

public class ExpressionExtractor {

    enum State {
        OUTSIDE_EXPRESSION,
        EXPRESSION_START,
        INSIDE_EXPRESSION
    }

    public static final String EXPR_PREFIX = "${concord:";

    public static List<String> collectExpressions(String input) {
        List<String> expressions = new ArrayList<>();
        StringBuilder currentExpression = new StringBuilder();
        State state = State.OUTSIDE_EXPRESSION;
        int curlyBraceCount = 0;

        for (char c : input.toCharArray()) {
            switch (state) {
                case OUTSIDE_EXPRESSION:
                    if (c == '$') {
                        currentExpression = new StringBuilder();
                        currentExpression.append(c);
                        state = State.EXPRESSION_START;
                    }
                    break;
                case EXPRESSION_START:
                    if (c == '$') {
                        currentExpression = new StringBuilder();
                    }

                    currentExpression.append(c);
                    String current = currentExpression.toString();
                    if (current.length() == EXPR_PREFIX.length()) {
                        if (current.equals(EXPR_PREFIX)) {
                            state = State.INSIDE_EXPRESSION;
                            curlyBraceCount = 0;
                        } else {
                            state = State.OUTSIDE_EXPRESSION;
                        }
                    }
                    break;
                case INSIDE_EXPRESSION:
                    currentExpression.append(c);
                    if (c == '{') {
                        curlyBraceCount++;
                    } else if (c == '}') {
                        if (curlyBraceCount > 0) {
                            curlyBraceCount--;
                        } else {
                            expressions.add(currentExpression.toString());
                            state = State.OUTSIDE_EXPRESSION;
                        }
                    }
                    break;
            }
        }

        return expressions;
    }
}
