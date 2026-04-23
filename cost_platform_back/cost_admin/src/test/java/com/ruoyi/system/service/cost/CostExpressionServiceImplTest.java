package com.ruoyi.system.service.cost;

import com.ruoyi.system.service.impl.cost.CostExpressionServiceImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CostExpressionServiceImplTest {
    private final CostExpressionServiceImpl expressionService = new CostExpressionServiceImpl();

    @Test
    void shouldKeepLiteralDivisionPrecisionForConstantExpressions() {
        Object result = expressionService.evaluate("round((21700 / 6) * 2, 2)", new LinkedHashMap<>());

        assertThat(result).isInstanceOf(Number.class);
        assertThat(new BigDecimal(String.valueOf(result))).isEqualByComparingTo("7233.33");
    }

    @Test
    void shouldKeepRatioPrecisionForIntegerInputsInNamespaceContext() {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("DUTY_TEAM_REQUIRED_ATTENDANCE", 10);
        variables.put("ALL_TEAMS_REQUIRED_ATTENDANCE", 20);
        variables.put("DUTY_TEAM_ACTUAL_ATTENDANCE", 10);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("V", variables);

        Object result = expressionService.evaluate(
                "(V.DUTY_TEAM_REQUIRED_ATTENDANCE / max(V.ALL_TEAMS_REQUIRED_ATTENDANCE, 1))"
                        + " * (V.DUTY_TEAM_ACTUAL_ATTENDANCE / max(V.DUTY_TEAM_REQUIRED_ATTENDANCE, 1))",
                context);

        assertThat(result).isInstanceOf(Number.class);
        assertThat(new BigDecimal(String.valueOf(result))).isEqualByComparingTo("0.5");
    }

    @Test
    void shouldSupportNumericComparisonsAfterLiteralNormalization() {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("ATTENDANCE_DAYS", 6);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("V", variables);

        Object result = expressionService.evaluate("if(V.ATTENDANCE_DAYS >= 6, 1, 0)", context);

        assertThat(result).isInstanceOf(Number.class);
        assertThat(new BigDecimal(String.valueOf(result))).isEqualByComparingTo("1");
    }

    @Test
    void shouldSupportBillMonthRegexMatchingInCommonNamespace() {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("SEASONAL_SUBSIDY_EQUIV", 2);

        Map<String, Object> common = new LinkedHashMap<>();
        common.put("billMonth", "2026-12");

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("V", variables);
        context.put("C", common);

        Object billMonth = expressionService.evaluate("C.billMonth", context);
        assertThat(String.valueOf(billMonth)).isEqualTo("2026-12");

        assertThat(expressionService.evaluate("'2026-12' matches '.*-(12|01|02|03)$'", new LinkedHashMap<>()))
                .isEqualTo(true);

        assertThat(invokeRewriteExpression("C.billMonth matches '.*-(12|01|02|03)$'"))
                .isEqualTo("C.billMonth matches '.*-(12|01|02|03)$'");

        Object matches = expressionService.evaluate("C.billMonth matches '.*-(12|01|02|03)$'", context);
        assertThat(matches).isEqualTo(true);

        Object result = expressionService.evaluate(
                "if((C.billMonth matches '.*-(12|01|02|03)$'), round(500 * V.SEASONAL_SUBSIDY_EQUIV, 2), 0)",
                context);

        assertThat(result).isInstanceOf(Number.class);
        assertThat(new BigDecimal(String.valueOf(result))).isEqualByComparingTo("1000");
    }

    private String invokeRewriteExpression(String expression) {
        try {
            Method method = CostExpressionServiceImpl.class.getDeclaredMethod("rewriteExpression", String.class);
            method.setAccessible(true);
            return String.valueOf(method.invoke(expressionService, expression));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
