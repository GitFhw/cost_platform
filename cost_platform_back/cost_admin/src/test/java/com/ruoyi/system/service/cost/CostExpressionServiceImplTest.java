package com.ruoyi.system.service.cost;

import com.ruoyi.system.service.impl.cost.CostExpressionServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CostExpressionServiceImplTest
{
    private final CostExpressionServiceImpl expressionService = new CostExpressionServiceImpl();

    @Test
    void shouldKeepRatioPrecisionForIntegerInputsInNamespaceContext()
    {
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
}
