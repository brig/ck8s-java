package ca.vanzyl.ck8s.asserts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.el.*;
import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.Map;

public class ExpressionEvaluator
{

    private final static Logger log = LoggerFactory.getLogger(ExpressionEvaluator.class);

    private final ExpressionFactory expressionFactory = ExpressionFactory.newInstance();

    private static String findVariableName(PropertyNotFoundException e)
    {
        String msg = e.getMessage();
        if (msg == null) {
            return null;
        }

        if (msg.startsWith("ELResolver cannot handle a null base Object with identifier ")) {
            return msg.substring("ELResolver cannot handle a null base Object with identifier ".length());
        }

        return null;
    }

    public <T> T eval(String expr, Map<String, Object> variables, Class<T> type)
    {
        try {
            ELResolver r = createResolver(variables);

            StandardELContext sc = new StandardELContext(expressionFactory);
            sc.putContext(ExpressionFactory.class, expressionFactory);
            sc.addELResolver(r);

            ValueExpression x = expressionFactory.createValueExpression(sc, expr, type);
            Object v = x.getValue(sc);
            return type.cast(v);
        }
        catch (PropertyNotFoundException e) {
            String name = findVariableName(e);
            if (name != null) {
                throw new RuntimeException(String.format("Undefined variable %s in expression %s. ", name, expr));
            }
            else {
                throw new RuntimeException(String.format("Undefined variable in expression %s. " +
                        "Details: %s", expr, e.getMessage()));
            }
        }
        catch (Exception e) {
            log.warn("interpolate ['{}'] -> error: {}", expr, e.getMessage());
            throw e;
        }
    }

    public boolean hasExpression(String v)
    {
        return v.contains("${");
    }

    private ELResolver createResolver(Map<String, Object> variables)
    {
        CompositeELResolver cr = new CompositeELResolver();
        cr.add(new VariableResolver(variables));
        return cr;
    }

    static class VariableResolver
            extends ELResolver
    {

        private final Map<String, Object> variables;

        public VariableResolver(Map<String, Object> variables)
        {
            this.variables = variables;
        }

        @Override
        public Class<?> getCommonPropertyType(ELContext context, Object base)
        {
            return Object.class;
        }

        @Override
        public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base)
        {
            return null;
        }

        @Override
        public Class<?> getType(ELContext context, Object base, Object property)
        {
            return Object.class;
        }

        @Override
        public Object getValue(ELContext context, Object base, Object property)
        {
            if (base == null && property instanceof String) {
                String k = (String) property;
                if (variables.containsKey(k)) {
                    context.setPropertyResolved(true);
                    return variables.get(k);
                }
            }

            return null;
        }

        @Override
        public boolean isReadOnly(ELContext context, Object base, Object property)
        {
            return true;
        }

        @Override
        public void setValue(ELContext context, Object base, Object property, Object value)
        {
        }
    }
}
