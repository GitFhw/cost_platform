package com.ruoyi.web.controller.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.cost.*;
import com.ruoyi.system.mapper.cost.*;
import com.ruoyi.system.service.impl.cost.CostDistributedLockSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CostPublishControllerManualIT {
    private static final DateTimeFormatter STAMP_FORMATTER = DateTimeFormatter.ofPattern("HHmmssSSS");
    private static final String SCENE_CODE = "SHOUGANG-ORE-HR-001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private CostVariableMapper variableMapper;

    @Autowired
    private CostFormulaMapper formulaMapper;

    @Autowired
    private CostRuleMapper ruleMapper;

    @Autowired
    private CostFeeMapper feeMapper;

    @Autowired
    private CostSceneMapper sceneMapper;

    @Autowired
    private CostSimulationRecordMapper simulationRecordMapper;

    @Autowired
    private CostPublishVersionMapper publishVersionMapper;

    @Autowired
    private CostDistributedLockSupport distributedLockSupport;

    @Test
    void shouldBlockPublishPrecheckWhenFormulaReferencesAreNotGoverned() throws Exception {
        Long sceneId = requireSceneId();
        CostFeeItem fee = findAnyEnabledFee(sceneId);
        String stamp = LocalTime.now().format(STAMP_FORMATTER);

        CostVariable variableMissingCode = buildFormulaVariable(sceneId,
                "TMPPUB_VAR_NOCODE_" + stamp, "temp formula var without code " + stamp);
        variableMissingCode.setFormulaCode("");
        variableMapper.insert(variableMissingCode);

        CostVariable variableMissingAsset = buildFormulaVariable(sceneId,
                "TMPPUB_VAR_BADCODE_" + stamp, "temp formula var bad asset " + stamp);
        variableMissingAsset.setFormulaCode("TMPPUB_FORMULA_BAD_" + stamp);
        variableMapper.insert(variableMissingAsset);

        CostRule ruleMissingCode = buildFormulaRule(sceneId, fee.getFeeId(),
                "TMPPUB_RULE_NOCODE_" + stamp, "temp formula rule without code " + stamp);
        ruleMissingCode.setAmountFormulaCode("");
        ruleMapper.insert(ruleMissingCode);

        CostRule ruleMissingAsset = buildFormulaRule(sceneId, fee.getFeeId(),
                "TMPPUB_RULE_BADCODE_" + stamp, "temp formula rule bad asset " + stamp);
        ruleMissingAsset.setAmountFormulaCode("TMPPUB_FORMULA_BAD_" + stamp);
        ruleMapper.insert(ruleMissingAsset);

        String token = loginAndGetToken();
        JsonNode precheck = readData(mockMvc.perform(get("/cost/publish/precheck/{sceneId}", sceneId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(precheck.path("publishable").asBoolean()).isFalse();
        assertThat(precheck.path("blockingCount").asLong()).isGreaterThan(0L);
        assertCheckMessageContains(precheck.path("items"), "FORMULA_VARIABLE_CODE_MISSING", variableMissingCode.getVariableCode());
        assertCheckMessageContains(precheck.path("items"), "FORMULA_VARIABLE_ASSET_MISSING", variableMissingAsset.getVariableCode());
        assertCheckMessageContains(precheck.path("items"), "FORMULA_RULE_CODE_MISSING", ruleMissingCode.getRuleCode());
        assertCheckMessageContains(precheck.path("items"), "FORMULA_RULE_ASSET_MISSING", ruleMissingAsset.getRuleCode());
    }

    @Test
    void shouldBlockPublishPrecheckWhenFormulaVariableDependenciesAreBroken() throws Exception {
        Long sceneId = requireSceneId();
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        String token = loginAndGetToken();

        CostFormula missingVariableFormula = buildFormula(sceneId, "TMPPUB_FORMULA_MISSINGVAR_" + stamp, "V.MISSING_VAR_" + stamp + " + 1");
        formulaMapper.insert(missingVariableFormula);
        CostVariable missingVariable = buildFormulaVariable(sceneId,
                "TMPPUB_VAR_MISSINGVAR_" + stamp, "temp formula var missing dep " + stamp);
        missingVariable.setFormulaCode(missingVariableFormula.getFormulaCode());
        missingVariable.setFormulaExpr(missingVariableFormula.getFormulaExpr());
        variableMapper.insert(missingVariable);

        CostFormula missingFeeFormula = buildFormula(sceneId, "TMPPUB_FORMULA_MISSINGFEE_" + stamp, "F['MISSING_FEE_" + stamp + "'] + 1");
        formulaMapper.insert(missingFeeFormula);
        CostVariable missingFee = buildFormulaVariable(sceneId,
                "TMPPUB_VAR_MISSINGFEE_" + stamp, "temp formula var missing fee " + stamp);
        missingFee.setFormulaCode(missingFeeFormula.getFormulaCode());
        missingFee.setFormulaExpr(missingFeeFormula.getFormulaExpr());
        variableMapper.insert(missingFee);

        CostFormula cycleFormulaA = buildFormula(sceneId, "TMPPUB_FORMULA_CYCLE_A_" + stamp, "V.TMPPUB_VAR_CYCLE_B_" + stamp + " + 1");
        CostFormula cycleFormulaB = buildFormula(sceneId, "TMPPUB_FORMULA_CYCLE_B_" + stamp, "V.TMPPUB_VAR_CYCLE_A_" + stamp + " + 1");
        formulaMapper.insert(cycleFormulaA);
        formulaMapper.insert(cycleFormulaB);

        CostVariable cycleA = buildFormulaVariable(sceneId,
                "TMPPUB_VAR_CYCLE_A_" + stamp, "temp cycle var A " + stamp);
        cycleA.setFormulaCode(cycleFormulaA.getFormulaCode());
        cycleA.setFormulaExpr(cycleFormulaA.getFormulaExpr());
        variableMapper.insert(cycleA);

        CostVariable cycleB = buildFormulaVariable(sceneId,
                "TMPPUB_VAR_CYCLE_B_" + stamp, "temp cycle var B " + stamp);
        cycleB.setFormulaCode(cycleFormulaB.getFormulaCode());
        cycleB.setFormulaExpr(cycleFormulaB.getFormulaExpr());
        variableMapper.insert(cycleB);

        JsonNode precheck = readData(mockMvc.perform(get("/cost/publish/precheck/{sceneId}", sceneId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(precheck.path("publishable").asBoolean()).isFalse();
        assertCheckMessageContains(precheck.path("items"), "FORMULA_DEPENDENCY_VARIABLE_MISSING", missingVariable.getVariableCode());
        assertCheckMessageContains(precheck.path("items"), "FORMULA_DEPENDENCY_FEE_MISSING", missingFee.getVariableCode());
        assertCheckMessageContains(precheck.path("items"), "FORMULA_DEPENDENCY_CYCLE", cycleA.getVariableCode());
    }

    @Test
    void shouldTreatDraftSimulationAsPrecheckEvidenceWhenSceneHasNoActiveVersion() throws Exception {
        Long sceneId = requireSceneId();
        CostScene scene = sceneMapper.selectById(sceneId);
        Long previousActiveVersionId = scene == null ? null : scene.getActiveVersionId();
        simulationRecordMapper.delete(Wrappers.<CostSimulationRecord>lambdaQuery()
                .eq(CostSimulationRecord::getSceneId, sceneId));
        sceneMapper.update(null, Wrappers.<CostScene>lambdaUpdate()
                .eq(CostScene::getSceneId, sceneId)
                .set(CostScene::getActiveVersionId, null));

        try {
            String token = loginAndGetToken();
            String authorization = "Bearer " + token;

            JsonNode beforePrecheck = readData(mockMvc.perform(get("/cost/publish/precheck/{sceneId}", sceneId)
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(findCheckItem(beforePrecheck.path("items"), "FIRST_RELEASE_NO_SIMULATION")).isNotNull();

            JsonNode template = readData(mockMvc.perform(get("/cost/run/input-template")
                            .header("Authorization", authorization)
                            .param("sceneId", String.valueOf(sceneId))
                            .param("taskType", "SIMULATION"))
                    .andExpect(status().isOk())
                    .andReturn());

            ObjectNode executeBody = objectMapper.createObjectNode();
            executeBody.put("sceneId", sceneId);
            executeBody.put("inputJson", template.path("inputJson").asText("{}"));

            JsonNode simulation = readData(mockMvc.perform(post("/cost/run/simulation/execute")
                            .header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(executeBody)))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(simulation.path("record").path("status").asText()).isEqualTo("SUCCESS");
            assertThat(simulation.path("record").path("versionId").isNull()).isTrue();

            JsonNode afterPrecheck = readData(mockMvc.perform(get("/cost/publish/precheck/{sceneId}", sceneId)
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(findCheckItem(afterPrecheck.path("items"), "FIRST_RELEASE_SIMULATION_READY")).isNotNull();
            assertThat(findCheckItem(afterPrecheck.path("items"), "FIRST_RELEASE_NO_SIMULATION")).isNull();
        } finally {
            sceneMapper.update(null, Wrappers.<CostScene>lambdaUpdate()
                    .eq(CostScene::getSceneId, sceneId)
                    .set(CostScene::getActiveVersionId, previousActiveVersionId));
        }
    }

    @Test
    void shouldBlockPublishActivateAndRollbackWhenSceneVersioningLockExists() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);

        Long versionId = publishScene(sceneId, authorization, "lock-baseline-" + stamp);
        String lockKey = distributedLockSupport.buildSceneVersioningLockKey(sceneId);
        redisCache.setCacheObject(lockKey, "itest-lock", 30, TimeUnit.SECONDS);

        try {
            ObjectNode publishBody = objectMapper.createObjectNode();
            publishBody.put("sceneId", sceneId);
            publishBody.put("publishDesc", "lock-regression-" + stamp);
            publishBody.put("activateNow", false);

            JsonNode publishResponse = readBody(mockMvc.perform(post("/cost/publish")
                            .header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(publishBody)))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(publishResponse.path("code").asInt()).isEqualTo(500);
            assertThat(publishResponse.path("msg").asText()).contains("发布/生效/回滚");

            JsonNode activateResponse = readBody(mockMvc.perform(put("/cost/publish/activate/{versionId}", versionId)
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(activateResponse.path("code").asInt()).isEqualTo(500);
            assertThat(activateResponse.path("msg").asText()).contains("发布/生效/回滚");

            JsonNode rollbackResponse = readBody(mockMvc.perform(put("/cost/publish/rollback/{versionId}", versionId)
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(rollbackResponse.path("code").asInt()).isEqualTo(500);
            assertThat(rollbackResponse.path("msg").asText()).contains("发布/生效/回滚");
        } finally {
            redisCache.deleteObject(lockKey);
        }
    }

    private Long requireSceneId() {
        CostScene scene = sceneMapper.selectOne(Wrappers.<CostScene>lambdaQuery()
                .eq(CostScene::getSceneCode, SCENE_CODE));
        assertThat(scene).as("missing seeded real-cost scene %s", SCENE_CODE).isNotNull();
        return scene.getSceneId();
    }

    private CostFeeItem findAnyEnabledFee(Long sceneId) {
        CostFeeItem query = new CostFeeItem();
        query.setSceneId(sceneId);
        query.setStatus("0");
        return feeMapper.selectFeeOptions(query).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("missing enabled fees in seeded real-cost scene"));
    }

    private Long publishScene(Long sceneId, String authorization, String publishDesc) throws Exception {
        Long previousLatestVersionId = latestVersionId(sceneId);

        ObjectNode publishBody = objectMapper.createObjectNode();
        publishBody.put("sceneId", sceneId);
        publishBody.put("publishDesc", publishDesc);
        publishBody.put("activateNow", false);

        JsonNode publish = readBody(mockMvc.perform(post("/cost/publish")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(publishBody)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(publish.path("code").asInt()).isEqualTo(200);

        CostPublishVersion latestVersion = publishVersionMapper.selectLatestVersionByScene(sceneId);
        assertThat(latestVersion).isNotNull();
        if (previousLatestVersionId != null) {
            assertThat(latestVersion.getVersionId()).isNotEqualTo(previousLatestVersionId);
        }
        return latestVersion.getVersionId();
    }

    private Long latestVersionId(Long sceneId) {
        CostPublishVersion latestVersion = publishVersionMapper.selectLatestVersionByScene(sceneId);
        return latestVersion == null ? null : latestVersion.getVersionId();
    }

    private CostVariable buildFormulaVariable(Long sceneId, String variableCode, String variableName) {
        CostVariable variable = new CostVariable();
        variable.setSceneId(sceneId);
        variable.setVariableCode(variableCode);
        variable.setVariableName(variableName);
        variable.setVariableType("FORMULA");
        variable.setSourceType("FORMULA");
        variable.setFormulaExpr("1");
        variable.setDataType("NUMBER");
        variable.setDefaultValue("0");
        variable.setPrecisionScale(2);
        variable.setStatus("0");
        variable.setSortNo(990);
        variable.setCreateBy("itest");
        variable.setUpdateBy("itest");
        return variable;
    }

    private CostRule buildFormulaRule(Long sceneId, Long feeId, String ruleCode, String ruleName) {
        CostRule rule = new CostRule();
        rule.setSceneId(sceneId);
        rule.setFeeId(feeId);
        rule.setRuleCode(ruleCode);
        rule.setRuleName(ruleName);
        rule.setRuleType("FORMULA");
        rule.setConditionLogic("AND");
        rule.setPriority(1);
        rule.setPricingMode("TYPED");
        rule.setAmountFormula("1");
        rule.setStatus("0");
        rule.setSortNo(990);
        rule.setCreateBy("itest");
        rule.setUpdateBy("itest");
        return rule;
    }

    private CostFormula buildFormula(Long sceneId, String formulaCode, String formulaExpr) {
        CostFormula formula = new CostFormula();
        formula.setSceneId(sceneId);
        formula.setFormulaCode(formulaCode);
        formula.setFormulaName("publish-precheck-formula-" + formulaCode);
        formula.setFormulaDesc("publish precheck formula dependency regression");
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

    private void assertCheckMessageContains(JsonNode items, String code, String expectedText) {
        JsonNode matched = findCheckItem(items, code);
        assertThat(matched).as("missing publish precheck item %s", code).isNotNull();
        assertThat(matched.path("message").asText()).contains(expectedText);
    }

    private JsonNode findCheckItem(JsonNode items, String code) {
        Iterator<JsonNode> iterator = items.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            if (code.equals(item.path("code").asText())) {
                return item;
            }
        }
        return null;
    }

    private String loginAndGetToken() throws Exception {
        JsonNode captcha = readBody(mockMvc.perform(get("/captchaImage"))
                .andExpect(status().isOk())
                .andReturn());
        String uuid = captcha.path("uuid").asText();
        Object captchaCode = redisCache.getCacheObject(CacheConstants.CAPTCHA_CODE_KEY + uuid);
        String code = captchaCode == null ? "" : String.valueOf(captchaCode);

        ObjectNode loginBody = objectMapper.createObjectNode();
        loginBody.put("username", "admin");
        loginBody.put("password", "admin123");
        loginBody.put("code", code);
        loginBody.put("uuid", uuid);

        JsonNode login = readBody(mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(loginBody)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(login.path("code").asInt()).isEqualTo(200);
        return login.path("token").asText();
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return readBody(result).path("data");
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(content);
    }
}
