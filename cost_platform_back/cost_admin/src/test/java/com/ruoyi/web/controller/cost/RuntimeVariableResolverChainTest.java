package com.ruoyi.web.controller.cost;

import com.ruoyi.system.domain.vo.CostExpressionAnalysisVo;
import com.ruoyi.system.service.cost.ICostExpressionService;
import com.ruoyi.system.service.cost.variable.runtime.*;
import com.ruoyi.system.service.impl.cost.CostRunServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeVariableResolverChainTest {
    @Test
    void shouldResolveAndConvertRuntimeVariableThroughResolutionService() {
        RuntimeVariableResolverChain chain = new RuntimeVariableResolverChain(
                new FormulaRuntimeVariableResolver(),
                new RemoteRuntimeVariableResolver(),
                new InputRuntimeVariableResolver(),
                new DictRuntimeVariableResolver(),
                new FallbackRuntimeVariableResolver());
        RuntimeVariableResolutionService service = new RuntimeVariableResolutionService(chain, new FakeExpressionService());

        CostRunServiceImpl.RuntimeVariable input = new CostRunServiceImpl.RuntimeVariable();
        input.variableCode = "INPUT_A";
        input.sourceType = "INPUT";
        input.dataPath = "inputA";

        CostRunServiceImpl.RuntimeVariable formula = new CostRunServiceImpl.RuntimeVariable();
        formula.variableCode = "FORMULA_A";
        formula.sourceType = "FORMULA";
        formula.formulaCode = "FORMULA_A";

        CostRunServiceImpl.RuntimeFormula runtimeFormula = new CostRunServiceImpl.RuntimeFormula();
        runtimeFormula.formulaCode = "FORMULA_A";
        runtimeFormula.formulaExpr = "V.INPUT_A + 1";

        CostRunServiceImpl.RuntimeSnapshot snapshot = new CostRunServiceImpl.RuntimeSnapshot();
        snapshot.variablesByCode.put(input.variableCode, input);
        snapshot.variablesByCode.put(formula.variableCode, formula);
        snapshot.formulasByCode.put(runtimeFormula.formulaCode, runtimeFormula);

        LinkedHashMap<String, Object> computedValues = new LinkedHashMap<>();
        Object value = service.resolve(
                snapshot,
                formula,
                Map.of("inputA", 5),
                computedValues,
                snapshot.variablesByCode,
                new LinkedHashSet<>(),
                (variable, baseContext) -> null,
                (rawValue, variable) -> "FORMULA_A".equals(variable.variableCode) ? "converted-" + rawValue : rawValue);

        assertThat(value).isEqualTo("converted-6");
        assertThat(computedValues)
                .containsEntry("INPUT_A", 5)
                .containsEntry("FORMULA_A", "converted-6");
    }

    @Test
    void shouldResolveFormulaVariableThroughDependencyAwareChain() {
        RuntimeVariableResolverChain chain = new RuntimeVariableResolverChain(
                new FormulaRuntimeVariableResolver(),
                new RemoteRuntimeVariableResolver(),
                new InputRuntimeVariableResolver(),
                new DictRuntimeVariableResolver(),
                new FallbackRuntimeVariableResolver());

        CostRunServiceImpl.RuntimeVariable input = new CostRunServiceImpl.RuntimeVariable();
        input.variableCode = "INPUT_A";
        input.sourceType = "INPUT";
        input.dataPath = "inputA";

        CostRunServiceImpl.RuntimeVariable formula = new CostRunServiceImpl.RuntimeVariable();
        formula.variableCode = "FORMULA_A";
        formula.sourceType = "FORMULA";
        formula.formulaCode = "FORMULA_A";

        CostRunServiceImpl.RuntimeSnapshot snapshot = new CostRunServiceImpl.RuntimeSnapshot();
        snapshot.variablesByCode.put(input.variableCode, input);
        snapshot.variablesByCode.put(formula.variableCode, formula);

        LinkedHashMap<String, Object> computedValues = new LinkedHashMap<>();
        Map<String, Object> baseContext = Map.of("inputA", 5);
        RuntimeVariableResolveContext context = new RuntimeVariableResolveContext(
                snapshot,
                formula,
                baseContext,
                computedValues,
                snapshot.variablesByCode,
                new LinkedHashSet<>());

        RuntimeVariableResolveSupport support = new RuntimeVariableResolveSupport() {
            @Override
            public Object resolveDependency(RuntimeVariableResolveContext ctx, CostRunServiceImpl.RuntimeVariable dependency) {
                Object dependencyValue = chain.resolve(ctx.withVariable(dependency), this);
                ctx.getComputedValues().put(dependency.variableCode, dependencyValue);
                return dependencyValue;
            }

            @Override
            public String resolveVariableFormula(CostRunServiceImpl.RuntimeSnapshot runtimeSnapshot,
                                                 CostRunServiceImpl.RuntimeVariable variable) {
                return "V.INPUT_A + 1";
            }

            @Override
            public Set<String> extractExpressionVariableCodes(String expression,
                                                              Map<String, CostRunServiceImpl.RuntimeVariable> variableMap) {
                return Set.of("INPUT_A");
            }

            @Override
            public Map<String, Object> mergeExpressionContext(Map<String, Object> originalBaseContext,
                                                              Map<String, Object> variableValues) {
                LinkedHashMap<String, Object> merged = new LinkedHashMap<>(originalBaseContext);
                merged.putAll(variableValues);
                return merged;
            }

            @Override
            public Object evaluateExpression(String expression, Map<String, Object> expressionContext) {
                return ((Number) expressionContext.get("INPUT_A")).intValue() + 1;
            }

            @Override
            public Object resolveRemoteVariableValue(CostRunServiceImpl.RuntimeVariable variable,
                                                     Map<String, Object> originalBaseContext) {
                return null;
            }

            @Override
            public Object resolveInputVariableValue(Map<String, Object> originalBaseContext,
                                                    CostRunServiceImpl.RuntimeVariable variable) {
                return originalBaseContext.get(variable.dataPath);
            }
        };

        Object value = chain.resolve(context, support);

        assertThat(value).isEqualTo(6);
        assertThat(computedValues).containsEntry("INPUT_A", 5);
    }

    @Test
    void shouldResolveRemoteVariableThroughRemoteResolver() {
        RuntimeVariableResolverChain chain = new RuntimeVariableResolverChain(
                new FormulaRuntimeVariableResolver(),
                new RemoteRuntimeVariableResolver(),
                new InputRuntimeVariableResolver(),
                new DictRuntimeVariableResolver(),
                new FallbackRuntimeVariableResolver());

        CostRunServiceImpl.RuntimeVariable remote = new CostRunServiceImpl.RuntimeVariable();
        remote.variableCode = "REMOTE_A";
        remote.sourceType = "REMOTE";

        RuntimeVariableResolveContext context = new RuntimeVariableResolveContext(
                new CostRunServiceImpl.RuntimeSnapshot(),
                remote,
                Collections.emptyMap(),
                new LinkedHashMap<>(),
                Collections.emptyMap(),
                new LinkedHashSet<>());

        RuntimeVariableResolveSupport support = new NoopRuntimeVariableResolveSupport() {
            @Override
            public Object resolveRemoteVariableValue(CostRunServiceImpl.RuntimeVariable variable,
                                                     Map<String, Object> baseContext) {
                return "remote-value";
            }
        };

        assertThat(chain.resolve(context, support)).isEqualTo("remote-value");
    }

    @Test
    void shouldResolveDictVariableThroughDictResolver() {
        RuntimeVariableResolverChain chain = new RuntimeVariableResolverChain(
                new FormulaRuntimeVariableResolver(),
                new RemoteRuntimeVariableResolver(),
                new InputRuntimeVariableResolver(),
                new DictRuntimeVariableResolver(),
                new FallbackRuntimeVariableResolver());

        CostRunServiceImpl.RuntimeVariable variable = new CostRunServiceImpl.RuntimeVariable();
        variable.variableCode = "DICT_A";
        variable.sourceType = "DICT";
        variable.dataPath = "dictValue";

        RuntimeVariableResolveContext context = new RuntimeVariableResolveContext(
                new CostRunServiceImpl.RuntimeSnapshot(),
                variable,
                Map.of("dictValue", "fallback-value"),
                new LinkedHashMap<>(),
                Collections.emptyMap(),
                new LinkedHashSet<>());

        RuntimeVariableResolveSupport support = new NoopRuntimeVariableResolveSupport() {
            @Override
            public Object resolveInputVariableValue(Map<String, Object> baseContext,
                                                    CostRunServiceImpl.RuntimeVariable runtimeVariable) {
                return baseContext.get(runtimeVariable.dataPath);
            }
        };

        assertThat(chain.resolve(context, support)).isEqualTo("fallback-value");
    }

    @Test
    void shouldFallbackToInputResolverForUnknownSourceType() {
        RuntimeVariableResolverChain chain = new RuntimeVariableResolverChain(
                new FormulaRuntimeVariableResolver(),
                new RemoteRuntimeVariableResolver(),
                new InputRuntimeVariableResolver(),
                new DictRuntimeVariableResolver(),
                new FallbackRuntimeVariableResolver());

        CostRunServiceImpl.RuntimeVariable variable = new CostRunServiceImpl.RuntimeVariable();
        variable.variableCode = "UNKNOWN_A";
        variable.sourceType = "UNKNOWN";
        variable.dataPath = "fallbackValue";

        RuntimeVariableResolveContext context = new RuntimeVariableResolveContext(
                new CostRunServiceImpl.RuntimeSnapshot(),
                variable,
                Map.of("fallbackValue", "fallback-value"),
                new LinkedHashMap<>(),
                Collections.emptyMap(),
                new LinkedHashSet<>());

        RuntimeVariableResolveSupport support = new NoopRuntimeVariableResolveSupport() {
            @Override
            public Object resolveInputVariableValue(Map<String, Object> baseContext,
                                                    CostRunServiceImpl.RuntimeVariable runtimeVariable) {
                return baseContext.get(runtimeVariable.dataPath);
            }
        };

        assertThat(chain.resolve(context, support)).isEqualTo("fallback-value");
    }

    @Test
    void shouldResolveRuntimeRemoteVariableValueFromPreparedContext() {
        RuntimeRemoteVariableValueService service = new RuntimeRemoteVariableValueService();

        CostRunServiceImpl.RuntimeVariable remote = new CostRunServiceImpl.RuntimeVariable();
        remote.variableCode = "REMOTE_A";
        remote.sourceType = "REMOTE";
        remote.sourceSystem = "PRODUCE";
        remote.dataPath = "mappedValue";
        remote.mappingConfigJson = "{\"matchBy\":{\"companyCode\":\"companyCode\"}}";

        Map<String, Object> baseContext = Map.of(
                "companyCode", "SHOUGANG",
                "remoteContext", Map.of(
                        "PRODUCE", Map.of(
                                "REMOTE_A", java.util.List.of(
                                        Map.of("companyCode", "OTHER", "mappedValue", 1),
                                        Map.of("companyCode", "SHOUGANG", "mappedValue", 2)))));

        assertThat(service.buildTemplatePath(remote)).isEqualTo("remoteContext.PRODUCE.REMOTE_A.mappedValue");
        assertThat(service.resolve(remote, baseContext)).isEqualTo(2);
    }

    private abstract static class NoopRuntimeVariableResolveSupport implements RuntimeVariableResolveSupport {
        @Override
        public Object resolveDependency(RuntimeVariableResolveContext context, CostRunServiceImpl.RuntimeVariable dependency) {
            return null;
        }

        @Override
        public String resolveVariableFormula(CostRunServiceImpl.RuntimeSnapshot snapshot,
                                             CostRunServiceImpl.RuntimeVariable variable) {
            return "";
        }

        @Override
        public Set<String> extractExpressionVariableCodes(String expression,
                                                          Map<String, CostRunServiceImpl.RuntimeVariable> variableMap) {
            return Collections.emptySet();
        }

        @Override
        public Map<String, Object> mergeExpressionContext(Map<String, Object> baseContext, Map<String, Object> computedValues) {
            return baseContext;
        }

        @Override
        public Object evaluateExpression(String expression, Map<String, Object> context) {
            return null;
        }

        @Override
        public Object resolveRemoteVariableValue(CostRunServiceImpl.RuntimeVariable variable, Map<String, Object> baseContext) {
            return null;
        }

        @Override
        public Object resolveInputVariableValue(Map<String, Object> baseContext, CostRunServiceImpl.RuntimeVariable variable) {
            return null;
        }
    }

    private static class FakeExpressionService implements ICostExpressionService {
        @Override
        public void validateExpression(String expression) {
        }

        @Override
        public void validateExpression(String expression, String namespaceScope) {
        }

        @Override
        public Object evaluate(String expression, Map<String, Object> context) {
            return ((Number) context.get("INPUT_A")).intValue() + 1;
        }

        @Override
        public CostExpressionAnalysisVo analyzeExpression(String expression) {
            return new CostExpressionAnalysisVo();
        }

        @Override
        public CostExpressionAnalysisVo analyzeExpression(String expression, String namespaceScope) {
            return new CostExpressionAnalysisVo();
        }

        @Override
        public Set<String> extractReferencedCodes(String expression, Collection<String> candidateCodes) {
            return Set.of("INPUT_A");
        }

        @Override
        public Set<String> extractFeeReferences(String expression) {
            return Collections.emptySet();
        }
    }
}
