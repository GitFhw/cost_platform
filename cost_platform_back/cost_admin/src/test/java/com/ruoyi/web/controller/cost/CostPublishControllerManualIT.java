package com.ruoyi.web.controller.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.cost.CostFeeItem;
import com.ruoyi.system.domain.cost.CostRule;
import com.ruoyi.system.domain.cost.CostScene;
import com.ruoyi.system.domain.cost.CostSimulationRecord;
import com.ruoyi.system.domain.cost.CostVariable;
import com.ruoyi.system.mapper.cost.CostFeeMapper;
import com.ruoyi.system.mapper.cost.CostRuleMapper;
import com.ruoyi.system.mapper.cost.CostSceneMapper;
import com.ruoyi.system.mapper.cost.CostSimulationRecordMapper;
import com.ruoyi.system.mapper.cost.CostVariableMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CostPublishControllerManualIT
{
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
    private CostRuleMapper ruleMapper;

    @Autowired
    private CostFeeMapper feeMapper;

    @Autowired
    private CostSceneMapper sceneMapper;

    @Autowired
    private CostSimulationRecordMapper simulationRecordMapper;

    @Test
    void shouldBlockPublishPrecheckWhenFormulaReferencesAreNotGoverned() throws Exception
    {
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
    void shouldTreatDraftSimulationAsPrecheckEvidenceWhenSceneHasNoActiveVersion() throws Exception
    {
        Long sceneId = requireSceneId();
        CostScene scene = sceneMapper.selectById(sceneId);
        Long previousActiveVersionId = scene == null ? null : scene.getActiveVersionId();
        simulationRecordMapper.delete(Wrappers.<CostSimulationRecord>lambdaQuery()
                .eq(CostSimulationRecord::getSceneId, sceneId));
        sceneMapper.update(null, Wrappers.<CostScene>lambdaUpdate()
                .eq(CostScene::getSceneId, sceneId)
                .set(CostScene::getActiveVersionId, null));

        try
        {
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
        }
        finally
        {
            sceneMapper.update(null, Wrappers.<CostScene>lambdaUpdate()
                    .eq(CostScene::getSceneId, sceneId)
                    .set(CostScene::getActiveVersionId, previousActiveVersionId));
        }
    }

    private Long requireSceneId()
    {
        CostScene scene = sceneMapper.selectOne(Wrappers.<CostScene>lambdaQuery()
                .eq(CostScene::getSceneCode, SCENE_CODE));
        assertThat(scene).as("missing seeded real-cost scene %s", SCENE_CODE).isNotNull();
        return scene.getSceneId();
    }

    private CostFeeItem findAnyEnabledFee(Long sceneId)
    {
        CostFeeItem query = new CostFeeItem();
        query.setSceneId(sceneId);
        query.setStatus("0");
        return feeMapper.selectFeeOptions(query).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("missing enabled fees in seeded real-cost scene"));
    }

    private CostVariable buildFormulaVariable(Long sceneId, String variableCode, String variableName)
    {
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

    private CostRule buildFormulaRule(Long sceneId, Long feeId, String ruleCode, String ruleName)
    {
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

    private void assertCheckMessageContains(JsonNode items, String code, String expectedText)
    {
        JsonNode matched = findCheckItem(items, code);
        assertThat(matched).as("missing publish precheck item %s", code).isNotNull();
        assertThat(matched.path("message").asText()).contains(expectedText);
    }

    private JsonNode findCheckItem(JsonNode items, String code)
    {
        Iterator<JsonNode> iterator = items.iterator();
        while (iterator.hasNext())
        {
            JsonNode item = iterator.next();
            if (code.equals(item.path("code").asText()))
            {
                return item;
            }
        }
        return null;
    }

    private String loginAndGetToken() throws Exception
    {
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

    private JsonNode readData(MvcResult result) throws Exception
    {
        return readBody(result).path("data");
    }

    private JsonNode readBody(MvcResult result) throws Exception
    {
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(content);
    }
}
