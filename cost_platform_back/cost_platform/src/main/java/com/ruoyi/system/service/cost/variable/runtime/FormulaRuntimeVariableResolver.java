package com.ruoyi.system.service.cost.variable.runtime;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.impl.cost.CostRunServiceImpl;
import org.springframework.stereotype.Component;

@Component
public class FormulaRuntimeVariableResolver implements RuntimeVariableResolver {
    private static final String SOURCE_TYPE_FORMULA = "FORMULA";

    @Override
    public boolean supports(RuntimeVariableResolveContext context) {
        CostRunServiceImpl.RuntimeVariable variable = context.getVariable();
        return variable != null && StringUtils.equals(SOURCE_TYPE_FORMULA, variable.sourceType);
    }

    @Override
    public Object resolve(RuntimeVariableResolveContext context,
                          RuntimeVariableResolveSupport support,
                          RuntimeVariableResolverChain chain) {
        CostRunServiceImpl.RuntimeVariable variable = context.getVariable();
        String formulaExpression = support.resolveVariableFormula(context.getSnapshot(), variable);
        for (String dependencyCode : support.extractExpressionVariableCodes(formulaExpression, context.getVariableMap())) {
            if (StringUtils.equals(variable.variableCode, dependencyCode)) {
                continue;
            }
            CostRunServiceImpl.RuntimeVariable dependency = context.getVariableMap().get(dependencyCode);
            if (dependency != null) {
                support.resolveDependency(context, dependency);
            }
        }
        return support.evaluateExpression(
                formulaExpression,
                support.mergeExpressionContext(context.getBaseContext(), context.getComputedValues()));
    }
}
