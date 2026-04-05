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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CostAuthorizationManualIT
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisCache redisCache;

    @Test
    void shouldRejectCostTaskAndGovernanceEndpointsForRegularUser() throws Exception
    {
        String token = loginAndGetToken("ry", "admin123");
        String authorization = "Bearer " + token;

        JsonNode taskList = readBody(mockMvc.perform(get("/cost/run/task/list")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(taskList.path("code").asInt()).isEqualTo(403);

        JsonNode cancelTask = readBody(mockMvc.perform(put("/cost/run/task/cancel/{taskId}", 1L)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(cancelTask.path("code").asInt()).isEqualTo(403);

        JsonNode alarmList = readBody(mockMvc.perform(get("/cost/governance/alarm/list")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(alarmList.path("code").asInt()).isEqualTo(403);

        JsonNode refreshCache = readBody(mockMvc.perform(put("/cost/governance/cache/refresh")
                        .header("Authorization", authorization)
                        .param("versionId", "1"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(refreshCache.path("code").asInt()).isEqualTo(403);
    }

    private String loginAndGetToken(String username, String password) throws Exception
    {
        JsonNode captcha = readBody(mockMvc.perform(get("/captchaImage"))
                .andExpect(status().isOk())
                .andReturn());
        String uuid = captcha.path("uuid").asText();
        Object captchaCode = redisCache.getCacheObject(CacheConstants.CAPTCHA_CODE_KEY + uuid);
        String code = captchaCode == null ? "" : String.valueOf(captchaCode);

        ObjectNode loginBody = objectMapper.createObjectNode();
        loginBody.put("username", username);
        loginBody.put("password", password);
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

    private JsonNode readBody(MvcResult result) throws Exception
    {
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(content);
    }
}
