package com.ruoyi.web.controller.cost;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CostVariableControllerManualIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisCache redisCache;

    @Test
    void shouldExposeSharedTemplateGovernanceSummary() throws Exception {
        String token = loginAndGetToken();
        JsonNode templates = readData(mockMvc.perform(get("/cost/variable/sharedTemplates")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(templates.isArray()).isTrue();
        assertThat(templates.size()).isGreaterThanOrEqualTo(3);

        JsonNode sgTemplate = findNodeByField(templates, "templateCode", "SG_OPERATION_BASE");
        assertThat(sgTemplate).isNotNull();
        assertThat(sgTemplate.path("templateName").asText()).isEqualTo("疏港共享因素模板");
        assertThat(sgTemplate.path("variableCount").asInt()).isEqualTo(4);
        assertThat(sgTemplate.path("appliedSceneCount").asInt()).isGreaterThanOrEqualTo(0);
        assertThat(sgTemplate.path("fullyAppliedSceneCount").asInt()).isGreaterThanOrEqualTo(0);
        assertThat(sgTemplate.path("matchedVariableCount").asInt()).isGreaterThanOrEqualTo(0);
        assertThat(sgTemplate.path("recentSceneNames").isArray()).isTrue();
        assertThat(sgTemplate.path("sceneSummaries").isArray()).isTrue();
        if (sgTemplate.path("recentSceneNames").size() > 0) {
            assertThat(sgTemplate.path("latestSceneName").asText()).isNotBlank();
        }
    }

    private String loginAndGetToken() throws Exception {
        JsonNode captcha = readBody(mockMvc.perform(get("/captchaImage"))
                .andExpect(status().isOk())
                .andReturn());
        String uuid = captcha.path("uuid").asText();
        Object captchaCode = redisCache.getCacheObject(CacheConstants.CAPTCHA_CODE_KEY + uuid);
        String code = captchaCode == null ? "" : String.valueOf(captchaCode);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("username", "admin");
        body.put("password", "admin123");
        body.put("code", code);
        body.put("uuid", uuid);

        JsonNode login = readBody(mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(login.path("code").asInt()).isEqualTo(200);
        return login.path("token").asText();
    }

    private JsonNode readData(MvcResult result) throws Exception {
        JsonNode root = readBody(result);
        assertThat(root.path("code").asInt()).isEqualTo(200);
        return root.path("data");
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private JsonNode findNodeByField(JsonNode array, String fieldName, String expectedValue) {
        if (array == null || !array.isArray()) {
            return null;
        }
        for (JsonNode node : array) {
            if (expectedValue.equals(node.path(fieldName).asText())) {
                return node;
            }
        }
        return null;
    }
}
