package ca.vanzyl.ck8s;

import com.walmartlabs.concord.runtime.v2.runner.context.ContextVariablesWithOverrides;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.EvalContext;
import com.walmartlabs.concord.runtime.v2.sdk.ExpressionEvaluator;

import java.util.*;

// Concord ignores expressions in object keys :(
public class ExpressionInterpolator {

    private final Context context;

    public ExpressionInterpolator(Context context) {
        this.context = context;
    }

    public <T> T eval(Object v, Map<String, Object> args, Class<T> type, boolean recursively) {
        if (v == null) {
            return null;
        }

        if (args == null) {
            args = Map.of();
        }

        return evalValue(v, args, type, recursively);
    }

    private <T> T evalValue(Object value, Map<String, Object> args, Class<T> type, boolean recursively) {
        if (value instanceof Map<?, ?> m) {
            return type.cast(evalMap(m, args, recursively));
        } else if (value instanceof Set<?> set) {
            if (set.isEmpty()) {
                return type.cast(set);
            }
            return type.cast(evalSet(set, args, recursively));
        } else if (value instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                return type.cast(collection);
            }
            return type.cast(evalList(collection, args, recursively));
        } else if (value != null && value.getClass().isArray()) {
            Object[] arr = (Object[]) value;
            if (arr.length == 0) {
                return type.cast(arr);
            }
            return type.cast(evalArray(arr, args, recursively));
        } else {
            return eval(context, value, args, type, recursively);
        }
    }

    private static <T> T eval(Context context, Object value, Map<String, Object> args, Class<T> type, boolean recursively) {
        var runtime = context.execution().runtime();
        var ee = runtime.getService(ExpressionEvaluator.class);
        EvalContext evalCtx;
        if (recursively) {
            evalCtx = EvalContext.builder()
                    .context(context)
                    .variables(new RecursiveContextVariablesWithOverrides(context, args))
                    .build();
            return type.cast(ee.eval(evalCtx, value, type));
        } else {
            if (args.isEmpty()) {
                return context.eval(value, type);
            } else {
                return context.eval(value, args, type);
            }
        }
    }

    private Map<Object, Object> evalMap(Map<?, ?> value, Map<String, Object> args, boolean recursively) {
        Map<Object, Object> result = new LinkedHashMap<>(value.size());
        for (Map.Entry<?, ?> e : value.entrySet()) {
            Object kk = e.getKey();
            if (kk instanceof String s) {
                if (isExpression(s)) {
                    kk = eval(context, s, args, String.class, recursively);
                }
            }

            Object vv = e.getValue();
            vv = eval(vv, args, Object.class, recursively);

            result.put(kk, vv);
        }
        return result;
    }

    private Set<Object> evalSet(Set<?> value, Map<String, Object> args, boolean recursively) {
        Set<Object> result = new LinkedHashSet<>(value.size());
        for (Object o : value) {
            result.add(eval(o, args, Object.class, recursively));
        }
        return result;
    }

    private List<Object> evalList(Collection<?> value, Map<String, Object> args, boolean recursively) {
        List<Object> result = new ArrayList<>(value.size());
        for (Object o : value) {
            result.add(eval(o, args, Object.class, recursively));
        }
        return result;
    }

    private Object[] evalArray(Object[] arr, Map<String, Object> args, boolean recursively) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = eval(arr[i], args, Object.class, recursively);
        }
        return arr;
    }

    private static boolean isExpression(String s) {
        if (s == null) {
            return false;
        }

        int i = s.indexOf("${");
        return i >= 0 && s.indexOf("}", i) > i;
    }

    private static boolean hasExpressionArray(Object[] arr) {
        return Arrays.stream(arr).anyMatch(ExpressionInterpolator::hasExpression);
    }

    private static boolean hasExpressionCollection(Collection<?> collection) {
        return collection.stream().anyMatch(ExpressionInterpolator::hasExpression);
    }

    private static boolean hasExpressionSet(Set<?> set) {
        return set.stream().anyMatch(ExpressionInterpolator::hasExpression);
    }

    private static boolean hasExpressionMap(Map<?, ?> m) {
        for (Map.Entry<?, ?> e : m.entrySet()) {
            Object kk = e.getKey();
            if (kk instanceof String s) {
                if (isExpression(s)) {
                    return true;
                }
            }

            var isExpr = hasExpression(e.getValue());
            if (isExpr) {
                return true;
            }
        }
        return false;
    }


    private static boolean hasExpression(Object value) {
        if (value == null) {
            return false;
        }

        if (value instanceof Map<?, ?> m) {
            return hasExpressionMap(m);
        } else if (value instanceof Set<?> set) {
            if (set.isEmpty()) {
                return false;
            }
            return hasExpressionSet(set);
        } else if (value instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                return false;
            }
            return hasExpressionCollection(collection);
        } else if (value.getClass().isArray()) {
            Object[] arr = (Object[]) value;
            if (arr.length == 0) {
                return false;
            }
            return hasExpressionArray(arr);
        } else if (value instanceof String s) {
            return isExpression(s);
        }

        return false;
    }

    private static class RecursiveContextVariablesWithOverrides extends ContextVariablesWithOverrides {

        private final Context context;
        private final Map<String, Object> overrides;

        public RecursiveContextVariablesWithOverrides(Context context, Map<String, Object> overrides) {
            super(context, overrides);
            this.context = context;
            this.overrides = overrides;
        }

        @Override
        public Object get(String key) {
            var result = super.get(key);
            if (result instanceof Map<?, ?> m) {
                return new EvalMap(m, context, overrides);
            }
            return result;
        }
    }

    private static class EvalMap extends LinkedHashMap<Object, Object> {

        private final Context context;
        private final Map<String, Object> overrides;

        public EvalMap(Map<?, ?> m, Context context, Map<String, Object> overrides) {
            super(m);
            this.context = context;
            this.overrides = overrides;
        }

        @Override
        public Object get(Object key) {
            var result = super.get(key);
            if (result instanceof String s) {
                if (isExpression(s)) {
                    result = eval(context, s, overrides, String.class, true);
                }
            }
            if (result instanceof Map<?, ?> m) {
                return new EvalMap(m, context, overrides);
            }
            return result;
        }
    }
}
