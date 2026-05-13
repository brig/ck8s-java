package ca.vanzyl.ck8s.asserts;

import ca.vanzyl.ck8s.asserts.json.JsonComparator;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;

import java.util.HashMap;
import java.util.Map;

public class ValueComparatorWithEl
        implements JsonComparator.ValueComparator
{

    private final ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();

    private final Variables variables;

    public ValueComparatorWithEl(Variables variables)
    {
        this.variables = variables;
    }

    @Override
    public boolean equals(String expected, Object actual)
    {
        if (expressionEvaluator.hasExpression(expected)) {
            Map<String, Object> exprVars = new HashMap<>(variables.toMap());
            exprVars.put("current", actual);
            return expressionEvaluator.eval(expected, exprVars, Boolean.class);
        }
        else {
            return expected.equals(String.valueOf(actual));
        }
    }
}
