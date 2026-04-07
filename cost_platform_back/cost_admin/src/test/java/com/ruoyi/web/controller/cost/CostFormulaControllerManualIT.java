package com.ruoyi.web.controller.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.cost.CostFormula;
import com.ruoyi.system.domain.cost.CostScene;
import com.ruoyi.system.mapper.cost.CostFormulaMapper;
import com.ruoyi.system.mapper.cost.CostFormulaVersionMapper;
import com.ruoyi.system.mapper.cost.CostSceneMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CostFormulaControllerManualIT
{
    private static final String SCENE_CODE = "SHOUGANG-ORE-HR-001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private CostSceneMapper sceneMapper;

    @Autowired
    private CostFormulaMapper formulaMapper;

    @Autowired
    private CostFormulaVersionMapper formulaVersionMapper;

    @Test
    void shouldRollbackFormulaToHistoricalVersion() throws Exception
    {
        Long sceneId = requireSceneId();
        CostFormula baseline = createLegacyFormula(sceneId);
        String originalExpr = baseline.getFormulaExpr();
        String originalBusinessFormula = baseline.getBusinessFormula();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;

        try
        {
            ObjectNode updateBody = objectMapper.valueToTree(baseline);
            updateBody.put("workbenchConfigJson", buildWorkbenchConfigJson(baseline));
            updateBody.put("businessFormula", "临时回滚验证公式");
            updateBody.put("formulaExpr", "round(100 / 3, 2)");

            JsonNode updateResponse = readBody(mockMvc.perform(put("/cost/formula")
                            .header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(updateBody)))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(updateResponse.path("code").asInt()).isEqualTo(200);

            JsonNode versionListAfterUpdate = readData(mockMvc.perform(get("/cost/formula/versions/{formulaId}", baseline.getFormulaId())
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(versionListAfterUpdate.isArray()).isTrue();
            assertThat(versionListAfterUpdate.size()).isGreaterThanOrEqualTo(2);
            JsonNode latestVersion = versionListAfterUpdate.path(0);
            JsonNode rollbackTarget = versionListAfterUpdate.path(1);
            assertThat(latestVersion.path("changeType").asText()).isEqualTo("UPDATE");
            assertThat(rollbackTarget.path("changeType").asText()).isEqualTo("CREATE");

            JsonNode rollbackResponse = readBody(mockMvc.perform(put("/cost/formula/version/rollback/{versionId}", rollbackTarget.path("versionId").asLong())
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(rollbackResponse.path("code").asInt()).isEqualTo(200);

            JsonNode currentFormula = readData(mockMvc.perform(get("/cost/formula/{formulaId}", baseline.getFormulaId())
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(currentFormula.path("formulaExpr").asText()).isEqualTo(originalExpr);
            assertThat(currentFormula.path("businessFormula").asText()).isEqualTo(originalBusinessFormula);

            JsonNode versionListAfterRollback = readData(mockMvc.perform(get("/cost/formula/versions/{formulaId}", baseline.getFormulaId())
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            JsonNode rollbackVersion = versionListAfterRollback.path(0);
            assertThat(rollbackVersion.path("changeType").asText()).isEqualTo("ROLLBACK");

            JsonNode rollbackVersionDetail = readData(mockMvc.perform(get("/cost/formula/version/{versionId}", rollbackVersion.path("versionId").asLong())
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(rollbackVersionDetail.path("formulaExpr").asText()).isEqualTo(originalExpr);
            assertThat(rollbackVersionDetail.path("businessFormula").asText()).isEqualTo(originalBusinessFormula);
        }
        finally
        {
            cleanupFormula(baseline.getFormulaId());
        }
    }

    private Long requireSceneId()
    {
        CostScene scene = sceneMapper.selectOne(Wrappers.<CostScene>lambdaQuery()
                .eq(CostScene::getSceneCode, SCENE_CODE));
        assertThat(scene).isNotNull();
        return scene.getSceneId();
    }

    private CostFormula createLegacyFormula(Long sceneId)
    {
        String formulaCode = "IT_ROLLBACK_" + System.currentTimeMillis();
        Date now = new Date();
        CostFormula formula = new CostFormula();
        formula.setSceneId(sceneId);
        formula.setFormulaCode(formulaCode);
        formula.setFormulaName("版本回滚联调公式");
        formula.setFormulaDesc("模拟历史老公式首次进入版本台账");
        formula.setBusinessFormula("1 + 2");
        formula.setFormulaExpr("1 + 2");
        formula.setAssetType("FORMULA");
        formula.setWorkbenchMode("EXPERT");
        formula.setWorkbenchPattern("IF_ELSE");
        formula.setTemplateCode("");
        formula.setWorkbenchConfigJson(null);
        formula.setNamespaceScope("V,C,I,F,T");
        formula.setReturnType("NUMBER");
        formula.setStatus("0");
        formula.setSortNo(999);
        formula.setCreateBy("manual-it");
        formula.setCreateTime(now);
        formula.setUpdateBy("manual-it");
        formula.setUpdateTime(now);
        formula.setRemark("自动化测试临时公式");
        formulaMapper.insert(formula);
        return formula;
    }

    private String buildWorkbenchConfigJson(CostFormula formula) throws Exception
    {
        ObjectNode config = objectMapper.createObjectNode();
        config.put("mode", "EXPERT");
        config.put("pattern", "IF_ELSE");
        config.put("templateCode", formula.getTemplateCode() == null ? "" : formula.getTemplateCode());
        config.put("conditionLogic", "AND");
        config.putArray("conditions");
        config.put("trueResultValue", "");
        config.put("falseResultValue", "");
        config.put("rangeVariableCode", "");
        config.putArray("ranges");
        config.put("defaultResultValue", "");
        config.put("businessFormula", formula.getBusinessFormula());
        config.put("formulaExpr", formula.getFormulaExpr());
        return objectMapper.writeValueAsString(config);
    }

    private void cleanupFormula(Long formulaId)
    {
        formulaVersionMapper.delete(Wrappers.<com.ruoyi.system.domain.cost.CostFormulaVersion>lambdaQuery()
                .eq(com.ruoyi.system.domain.cost.CostFormulaVersion::getFormulaId, formulaId));
        formulaMapper.deleteById(formulaId);
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
