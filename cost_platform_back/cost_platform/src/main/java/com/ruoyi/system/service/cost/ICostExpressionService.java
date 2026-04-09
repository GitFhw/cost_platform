package com.ruoyi.system.service.cost;

import java.util.Map;

/**
 * 统一表达式执行服务接口。
 *
 * <p>线程七开始统一承接公式、变量公式与规则公式的语法校验和执行入口，
 * 为后续公式编码引用、快照执行和追溯解释提供一致能力。</p>
 *
 * @author HwFan
 */
public interface ICostExpressionService {
    /**
     * 仅校验表达式语法。
     */
    void validateExpression(String expression);

    /**
     * 执行表达式。
     */
    Object evaluate(String expression, Map<String, Object> context);
}
