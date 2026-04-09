package com.ruoyi.system.service.impl.cost;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.cost.ICostExpressionService;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.Operation;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一表达式执行服务实现。
 *
 * <p>线程七将公式实验室、公式变量和规则金额公式统一接到该服务，
 * 由这里负责语法校验、函数改写、缓存编译和 V/C/I/F/T 命名空间上下文收口。</p>
 *
 * @author HwFan
 */
@Service
public class CostExpressionServiceImpl implements ICostExpressionService {
    private static final int DIVIDE_SCALE = 16;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();
    private final OperatorOverloader numericOperatorOverloader = new NumericOperatorOverloader();

    @Override
    public void validateExpression(String expression) {
        if (StringUtils.isEmpty(expression)) {
            throw new ServiceException("表达式不能为空");
        }
        try {
            compileExpression(expression);
        } catch (Exception e) {
            throw new ServiceException("表达式校验失败：" + e.getMessage());
        }
    }

    @Override
    public Object evaluate(String expression, Map<String, Object> context) {
        if (StringUtils.isEmpty(expression)) {
            return null;
        }
        try {
            StandardEvaluationContext evaluationContext = new StandardEvaluationContext(buildRootContext(context));
            evaluationContext.addPropertyAccessor(new MapAccessor());
            evaluationContext.setOperatorOverloader(numericOperatorOverloader);
            evaluationContext.setVariable("if", new CommonFunctions());
            evaluationContext.setVariable("max", new MaxFunctions());
            evaluationContext.setVariable("min", new MinFunctions());
            evaluationContext.setVariable("round", new CommonFunctions());
            evaluationContext.setVariable("between", new CommonFunctions());
            evaluationContext.setVariable("coalesce", new CommonFunctions());
            return compileExpression(expression).getValue(evaluationContext);
        } catch (Exception e) {
            throw new ServiceException("表达式执行失败：" + e.getMessage());
        }
    }

    /**
     * 构造统一根上下文，保证运行时至少存在 V/C/I/F/T 五个命名空间。
     */
    private Map<String, Object> buildRootContext(Map<String, Object> input) {
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        if (input != null) {
            for (Map.Entry<String, Object> entry : input.entrySet()) {
                root.put(entry.getKey(), normalizeContextValue(entry.getValue()));
            }
        }
        root.computeIfAbsent("V", key -> new LinkedHashMap<>());
        root.computeIfAbsent("C", key -> new LinkedHashMap<>());
        root.computeIfAbsent("I", key -> new LinkedHashMap<>());
        root.computeIfAbsent("F", key -> new LinkedHashMap<>());
        root.computeIfAbsent("T", key -> new LinkedHashMap<>());
        return root;
    }

    private Object normalizeContextValue(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean || value instanceof Character
                || value instanceof Enum<?>) {
            return value;
        }
        if (value instanceof BigInteger bigInteger) {
            return bigInteger.doubleValue();
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<Object, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                normalized.put(entry.getKey(), normalizeContextValue(entry.getValue()));
            }
            return normalized;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> normalized = new ArrayList<>(collection.size());
            for (Object item : collection) {
                normalized.add(normalizeContextValue(item));
            }
            return normalized;
        }
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            List<Object> normalized = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                normalized.add(normalizeContextValue(java.lang.reflect.Array.get(value, i)));
            }
            return normalized;
        }
        return value;
    }

    /**
     * 编译表达式并做缓存。
     */
    private Expression compileExpression(String expression) {
        return expressionCache.computeIfAbsent(expression, key -> parser.parseExpression(rewriteExpression(key)));
    }

    /**
     * 将平台表达式改写为 SpEL 可执行语法。
     */
    private String rewriteExpression(String source) {
        String expression = source.replace("&&", " and ").replace("||", " or ");
        expression = expression.replaceAll("\\bif\\s*\\(", "#if.choose(");
        expression = expression.replaceAll("\\bmax\\s*\\(", "#max.pick(");
        expression = expression.replaceAll("\\bmin\\s*\\(", "#min.pick(");
        expression = expression.replaceAll("\\bround\\s*\\(", "#round.scale(");
        expression = expression.replaceAll("\\bbetween\\s*\\(", "#between.hit(");
        expression = expression.replaceAll("\\bcoalesce\\s*\\(", "#coalesce.first(");
        return expression;
    }

    /**
     * 公共函数集合。
     */
    public static class CommonFunctions {
        /**
         * 条件选择函数。
         */
        public Object choose(Object condition, Object trueValue, Object falseValue) {
            return toBoolean(condition) ? trueValue : falseValue;
        }

        /**
         * 保留精度。
         */
        public BigDecimal scale(Object value, Object digits) {
            int precision = digits == null ? 2 : Integer.parseInt(String.valueOf(digits));
            return toBigDecimal(value).setScale(precision, RoundingMode.HALF_UP);
        }

        /**
         * 区间命中。
         */
        public boolean hit(Object value, Object start, Object end) {
            BigDecimal current = toBigDecimal(value);
            return current.compareTo(toBigDecimal(start)) >= 0 && current.compareTo(toBigDecimal(end)) <= 0;
        }

        /**
         * 取首个非空值。
         */
        public Object first(Object first, Object second) {
            return first != null ? first : second;
        }

        /**
         * 取首个非空值。
         */
        public Object first(Object first, Object second, Object third) {
            if (first != null) {
                return first;
            }
            return second != null ? second : third;
        }

        protected boolean toBoolean(Object value) {
            if (value instanceof Boolean bool) {
                return bool;
            }
            return Boolean.parseBoolean(String.valueOf(value));
        }

        protected BigDecimal toBigDecimal(Object value) {
            if (value == null) {
                return BigDecimal.ZERO;
            }
            if (value instanceof BigDecimal decimal) {
                return decimal;
            }
            return new BigDecimal(String.valueOf(value));
        }
    }

    /**
     * 最大值函数集合。
     */
    public static class MaxFunctions extends CommonFunctions {
        public Double pick(Object left, Object right) {
            return Math.max(toBigDecimal(left).doubleValue(), toBigDecimal(right).doubleValue());
        }

        public Double pick(Object first, Object second, Object third) {
            return pick(pick(first, second), third);
        }
    }

    /**
     * 最小值函数集合。
     */
    public static class MinFunctions extends CommonFunctions {
        public Double pick(Object left, Object right) {
            return Math.min(toBigDecimal(left).doubleValue(), toBigDecimal(right).doubleValue());
        }

        public Double pick(Object first, Object second, Object third) {
            return pick(pick(first, second), third);
        }
    }

    private static class NumericOperatorOverloader implements OperatorOverloader {
        private static boolean isNumericOperand(Object value) {
            return value instanceof Number || value instanceof BigInteger || value instanceof BigDecimal;
        }

        private static BigDecimal toBigDecimal(Object value) {
            if (value instanceof BigDecimal decimal) {
                return decimal;
            }
            if (value instanceof BigInteger bigInteger) {
                return new BigDecimal(bigInteger);
            }
            if (value instanceof Number) {
                return new BigDecimal(String.valueOf(value));
            }
            throw new IllegalArgumentException("Unsupported numeric operand: " + value);
        }

        @Override
        public boolean overridesOperation(Operation operation, Object leftOperand, Object rightOperand) {
            return switch (operation) {
                case ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULUS, POWER ->
                        isNumericOperand(leftOperand) && isNumericOperand(rightOperand);
                default -> false;
            };
        }

        @Override
        public Object operate(Operation operation, Object leftOperand, Object rightOperand) {
            BigDecimal left = toBigDecimal(leftOperand);
            BigDecimal right = toBigDecimal(rightOperand);
            return switch (operation) {
                case ADD -> left.add(right);
                case SUBTRACT -> left.subtract(right);
                case MULTIPLY -> left.multiply(right);
                case DIVIDE -> right.compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : left.divide(right, DIVIDE_SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
                case MODULUS -> right.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : left.remainder(right);
                case POWER -> left.pow(right.intValue());
                default -> throw new IllegalStateException("Unsupported numeric operation: " + operation);
            };
        }
    }
}
