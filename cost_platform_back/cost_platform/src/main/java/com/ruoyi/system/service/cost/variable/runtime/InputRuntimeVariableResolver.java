package com.ruoyi.system.service.cost.variable.runtime;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.impl.cost.CostRunServiceImpl;
import org.springframework.stereotype.Component;

@Component
public class InputRuntimeVariableResolver implements RuntimeVariableResolver {
    private static final String SOURCE_TYPE_INPUT = "INPUT";

    @Override
    public boolean supports(RuntimeVariableResolveContext context) {
        CostRunServiceImpl.RuntimeVariable variable = context.getVariable();
        return variable != null && StringUtils.equalsIgnoreCase(SOURCE_TYPE_INPUT, variable.sourceType);
    }

    @Override
    public Object resolve(RuntimeVariableResolveContext context,
                          RuntimeVariableResolveSupport support,
                          RuntimeVariableResolverChain chain) {
        return support.resolveInputVariableValue(context.getBaseContext(), context.getVariable());
    }
}
