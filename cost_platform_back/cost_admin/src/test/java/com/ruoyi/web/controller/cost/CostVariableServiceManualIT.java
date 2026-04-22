package com.ruoyi.web.controller.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.system.domain.cost.CostFormula;
import com.ruoyi.system.domain.cost.CostScene;
import com.ruoyi.system.domain.cost.CostVariable;
import com.ruoyi.system.mapper.cost.CostFormulaMapper;
import com.ruoyi.system.mapper.cost.CostSceneMapper;
import com.ruoyi.system.mapper.cost.CostVariableMapper;
import com.ruoyi.system.service.cost.ICostVariableService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CostVariableServiceManualIT {
    private static final DateTimeFormatter STAMP_FORMATTER = DateTimeFormatter.ofPattern("HHmmssSSS");
    private static final String SCENE_CODE = "SHOUGANG-ORE-HR-001";

    @Autowired
    private ICostVariableService variableService;

    @Autowired
    private CostVariableMapper variableMapper;

    @Autowired
    private CostSceneMapper sceneMapper;

    @Autowired
    private CostFormulaMapper formulaMapper;

    @Test
    void shouldNormalizeInputVariableFieldsThroughSourceHandlerChain() {
        Long sceneId = requireSceneId();
        String stamp = LocalTime.now().format(STAMP_FORMATTER);

        CostVariable variable = buildBaseVariable(sceneId, "TMP_VAR_INPUT_CHAIN_" + stamp, "input-chain-" + stamp);
        variable.setSourceType("INPUT");
        variable.setDictType("cost_variable_status");
        variable.setSourceSystem("PRODUCE");
        variable.setRemoteApi("https://example.internal/demo");
        variable.setRequestMethod("post");
        variable.setQueryConfigJson("{\"pageNum\":1}");
        variable.setRequestHeadersJson("{\"X-App\":\"demo\"}");
        variable.setBodyTemplateJson("{\"enabled\":true}");
        variable.setAuthType("BEARER");
        variable.setAuthConfigJson("{\"token\":\"demo\"}");
        variable.setDataPath("payload.code");
        variable.setResponseConfigJson("{\"listPath\":\"rows\"}");
        variable.setMappingConfigJson("{\"sourceCode\":\"code\"}");
        variable.setPageConfigJson("{\"pageNumKey\":\"pageNum\"}");
        variable.setAdapterType("PAGE_ENVELOPE");
        variable.setAdapterConfigJson("{\"notes\":\"demo\"}");
        variable.setSyncMode("MANUAL");
        variable.setCachePolicy("TTL");
        variable.setFallbackPolicy("USE_DEFAULT");
        variable.setFormulaExpr("1+1");
        variable.setFormulaCode("TMP_FORMULA");

        variableService.insertVariable(variable);
        CostVariable stored = variableMapper.selectById(variable.getVariableId());

        assertThat(stored.getSourceType()).isEqualTo("INPUT");
        assertThat(stored.getDictType()).isEmpty();
        assertThat(stored.getSourceSystem()).isEmpty();
        assertThat(stored.getDataPath()).isEqualTo("payload.code");
        assertThat(stored.getRemoteApi()).isEmpty();
        assertThat(stored.getRequestMethod()).isEqualTo("GET");
        assertThat(stored.getContentType()).isEqualTo("application/json");
        assertThat(stored.getAuthType()).isEqualTo("NONE");
        assertThat(stored.getSyncMode()).isEqualTo("REALTIME");
        assertThat(stored.getCachePolicy()).isEqualTo("MANUAL_REFRESH");
        assertThat(stored.getFallbackPolicy()).isEqualTo("FAIL_FAST");
        assertThat(stored.getAdapterType()).isEqualTo("STANDARD");
        assertThat(stored.getFormulaExpr()).isNull();
        assertThat(stored.getFormulaCode()).isEmpty();
    }

    @Test
    void shouldKeepDictVariableDataPathThroughSourceHandlerChain() {
        Long sceneId = requireSceneId();
        String stamp = LocalTime.now().format(STAMP_FORMATTER);

        CostVariable variable = buildBaseVariable(sceneId, "TMP_VAR_DICT_CHAIN_" + stamp, "dict-chain-" + stamp);
        variable.setSourceType("DICT");
        variable.setDictType("cost_variable_status");
        variable.setDataPath("cover.action");
        variable.setRemoteApi("https://example.internal/demo");
        variable.setFormulaCode("TMP_FORMULA");

        variableService.insertVariable(variable);
        CostVariable stored = variableMapper.selectById(variable.getVariableId());

        assertThat(stored.getSourceType()).isEqualTo("DICT");
        assertThat(stored.getDictType()).isEqualTo("cost_variable_status");
        assertThat(stored.getDataPath()).isEqualTo("cover.action");
        assertThat(stored.getRemoteApi()).isEmpty();
        assertThat(stored.getFormulaCode()).isEmpty();
    }

    @Test
    void shouldAllowBlankDataPathForFlatInputAndDictVariables() {
        Long sceneId = requireSceneId();
        String stamp = LocalTime.now().format(STAMP_FORMATTER);

        CostVariable input = buildBaseVariable(sceneId, "TMP_VAR_FLAT_INPUT_" + stamp, "flat-input-" + stamp);
        input.setSourceType("INPUT");
        input.setDataPath(" ");

        variableService.insertVariable(input);
        CostVariable storedInput = variableMapper.selectById(input.getVariableId());

        assertThat(storedInput.getSourceType()).isEqualTo("INPUT");
        assertThat(storedInput.getDataPath()).isEmpty();

        CostVariable dict = buildBaseVariable(sceneId, "TMP_VAR_FLAT_DICT_" + stamp, "flat-dict-" + stamp);
        dict.setSourceType("DICT");
        dict.setDictType("cost_variable_status");
        dict.setDataPath("");

        variableService.insertVariable(dict);
        CostVariable storedDict = variableMapper.selectById(dict.getVariableId());

        assertThat(storedDict.getSourceType()).isEqualTo("DICT");
        assertThat(storedDict.getDictType()).isEqualTo("cost_variable_status");
        assertThat(storedDict.getDataPath()).isEmpty();
    }

    @Test
    void shouldNormalizeRemoteAndFormulaVariableFieldsThroughSourceHandlerChain() {
        Long sceneId = requireSceneId();
        String stamp = LocalTime.now().format(STAMP_FORMATTER);

        CostVariable remote = buildBaseVariable(sceneId, "TMP_VAR_REMOTE_CHAIN_" + stamp, "remote-chain-" + stamp);
        remote.setSourceType("REMOTE");
        remote.setSourceSystem("PRODUCE");
        remote.setRemoteApi("https://example.internal/remote/list");
        remote.setDataPath("remote.amount");
        remote.setRequestMethod("post");
        remote.setContentType("");
        remote.setAuthType("");
        remote.setSyncMode("");
        remote.setCachePolicy("");
        remote.setFallbackPolicy("");
        remote.setAdapterType("");
        remote.setDictType("cost_variable_status");
        remote.setFormulaExpr("2+2");
        remote.setFormulaCode("TMP_FORMULA");
        remote.setQueryConfigJson("{}");
        remote.setRequestHeadersJson("{}");
        remote.setAuthConfigJson("{}");
        remote.setBodyTemplateJson("{}");
        remote.setResponseConfigJson("{}");
        remote.setMappingConfigJson("{}");
        remote.setPageConfigJson("{}");
        remote.setAdapterConfigJson("{}");

        variableService.insertVariable(remote);
        CostVariable storedRemote = variableMapper.selectById(remote.getVariableId());

        assertThat(storedRemote.getSourceType()).isEqualTo("REMOTE");
        assertThat(storedRemote.getSourceSystem()).isEqualTo("PRODUCE");
        assertThat(storedRemote.getRemoteApi()).isEqualTo("https://example.internal/remote/list");
        assertThat(storedRemote.getDataPath()).isEqualTo("remote.amount");
        assertThat(storedRemote.getRequestMethod()).isEqualTo("POST");
        assertThat(storedRemote.getContentType()).isEqualTo("application/json");
        assertThat(storedRemote.getAuthType()).isEqualTo("NONE");
        assertThat(storedRemote.getSyncMode()).isEqualTo("REALTIME");
        assertThat(storedRemote.getCachePolicy()).isEqualTo("MANUAL_REFRESH");
        assertThat(storedRemote.getFallbackPolicy()).isEqualTo("FAIL_FAST");
        assertThat(storedRemote.getAdapterType()).isEqualTo("STANDARD");
        assertThat(storedRemote.getDictType()).isEmpty();
        assertThat(storedRemote.getFormulaExpr()).isNull();
        assertThat(storedRemote.getFormulaCode()).isEmpty();

        CostFormula formula = buildFormula(sceneId, "TMP_FORMULA_CHAIN_" + stamp, "V.INPUT_A_" + stamp + " + 1");
        formulaMapper.insert(formula);

        CostVariable formulaVariable = buildBaseVariable(sceneId, "TMP_VAR_FORMULA_CHAIN_" + stamp, "formula-chain-" + stamp);
        formulaVariable.setSourceType("FORMULA");
        formulaVariable.setFormulaCode(formula.getFormulaCode());
        formulaVariable.setSourceSystem("PRODUCE");
        formulaVariable.setDictType("cost_variable_status");
        formulaVariable.setRemoteApi("https://example.internal/demo");

        variableService.insertVariable(formulaVariable);
        CostVariable storedFormula = variableMapper.selectById(formulaVariable.getVariableId());

        assertThat(storedFormula.getSourceType()).isEqualTo("FORMULA");
        assertThat(storedFormula.getFormulaCode()).isEqualTo(formula.getFormulaCode());
        assertThat(storedFormula.getFormulaExpr()).isEqualTo(formula.getFormulaExpr());
        assertThat(storedFormula.getSourceSystem()).isEmpty();
        assertThat(storedFormula.getDictType()).isEmpty();
        assertThat(storedFormula.getRemoteApi()).isEmpty();
        assertThat(storedFormula.getRequestMethod()).isEqualTo("GET");
    }

    private Long requireSceneId() {
        CostScene scene = sceneMapper.selectOne(Wrappers.<CostScene>lambdaQuery()
                .eq(CostScene::getSceneCode, SCENE_CODE));
        assertThat(scene).isNotNull();
        return scene.getSceneId();
    }

    private CostVariable buildBaseVariable(Long sceneId, String variableCode, String variableName) {
        CostVariable variable = new CostVariable();
        variable.setSceneId(sceneId);
        variable.setVariableCode(variableCode);
        variable.setVariableName(variableName);
        variable.setVariableType("TEXT");
        variable.setSourceType("INPUT");
        variable.setDataType("STRING");
        variable.setDefaultValue("");
        variable.setPrecisionScale(2);
        variable.setStatus("0");
        variable.setSortNo(990);
        variable.setCreateBy("itest");
        variable.setUpdateBy("itest");
        return variable;
    }

    private CostFormula buildFormula(Long sceneId, String formulaCode, String formulaExpr) {
        CostFormula formula = new CostFormula();
        formula.setSceneId(sceneId);
        formula.setFormulaCode(formulaCode);
        formula.setFormulaName("variable-source-chain-" + formulaCode);
        formula.setFormulaDesc("variable source chain regression");
        formula.setBusinessFormula(formulaExpr);
        formula.setFormulaExpr(formulaExpr);
        formula.setAssetType("FORMULA");
        formula.setWorkbenchMode("EXPERT");
        formula.setWorkbenchPattern("IF_ELSE");
        formula.setTemplateCode("");
        formula.setWorkbenchConfigJson("{}");
        formula.setNamespaceScope("V,F,I,C,T");
        formula.setReturnType("NUMBER");
        formula.setStatus("0");
        formula.setSortNo(990);
        formula.setCreateBy("itest");
        formula.setUpdateBy("itest");
        return formula;
    }
}
