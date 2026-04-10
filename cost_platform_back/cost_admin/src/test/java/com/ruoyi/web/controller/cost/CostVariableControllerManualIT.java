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
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
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

    @Autowired
    private RestTemplate costAccessRestTemplate;

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

    @Test
    void shouldTestRemoteConnectionAgainstConfiguredHttpEndpoint() throws Exception {
        String token = loginAndGetToken();
        MockRestServiceServer server = MockRestServiceServer.bindTo(costAccessRestTemplate).build();
        server.expect(once(), requestTo("https://example.internal/odd-work/code-list?pageNum=1&pageSize=20"))
                .andExpect(method(GET))
                .andExpect(header("Authorization", "Bearer demo-token"))
                .andExpect(header("Cookie", "SESSION=abc"))
                .andExpect(header("Referer", "http://172.16.10.8:88/produce/record/oddWorkRecord"))
                .andRespond(withSuccess("""
                        {
                          "code": 200,
                          "msg": "ok",
                          "rows": [
                            {
                              "projectCode": "ODD-01",
                              "projectName": "零工甲",
                              "companyCode": "SG",
                              "unitPrice": 12.5
                            }
                          ],
                          "total": 1
                        }
                        """, MediaType.APPLICATION_JSON));

        ObjectNode body = objectMapper.createObjectNode();
        body.put("variableCode", "ODD_WORK_PROJECT_REMOTE");
        body.put("sourceSystem", "PRODUCE");
        body.put("remoteApi", "https://example.internal/odd-work/code-list");
        body.put("requestMethod", "GET");
        body.put("contentType", "application/json");
        body.put("queryConfigJson", "{\"pageNum\":1}");
        body.put("requestHeadersJson", "{\"Referer\":\"http://172.16.10.8:88/produce/record/oddWorkRecord\",\"Cookie\":\"SESSION=abc\"}");
        body.put("authType", "BEARER");
        body.put("authConfigJson", "{\"token\":\"demo-token\"}");
        body.put("responseConfigJson", "{\"successPath\":\"code\",\"successValues\":[200],\"messagePath\":\"msg\",\"listPath\":\"rows\",\"totalPath\":\"total\"}");
        body.put("pageConfigJson", "{\"pageNumKey\":\"pageNum\",\"pageSizeKey\":\"pageSize\",\"previewPageNum\":1,\"previewPageSize\":20}");
        body.put("adapterType", "PAGE_ENVELOPE");

        JsonNode data = readData(mockMvc.perform(post("/cost/variable/remote/test")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(data.path("success").asBoolean()).isTrue();
        assertThat(data.path("statusCode").asInt()).isEqualTo(200);
        assertThat(data.path("rowCount").asInt()).isEqualTo(1);
        assertThat(data.path("requestMethod").asText()).isEqualTo("GET");
        assertThat(data.path("adapterType").asText()).isEqualTo("PAGE_ENVELOPE");
        server.verify();
    }

    @Test
    void shouldPreviewRemoteDataAgainstConfiguredHttpEndpoint() throws Exception {
        String token = loginAndGetToken();
        MockRestServiceServer server = MockRestServiceServer.bindTo(costAccessRestTemplate).build();
        server.expect(once(), requestTo("https://example.internal/odd-work/query"))
                .andExpect(method(POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header("X-App", "cost-platform"))
                .andExpect(content().json("""
                        {
                          "companyCode": "SG",
                          "enabled": true
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "status": "OK",
                          "message": "ok",
                          "data": {
                            "items": [
                              {
                                "code": "ODD-02",
                                "name": "零工乙",
                                "price": 18.8,
                                "company": {
                                  "code": "SG"
                                }
                              }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        ObjectNode body = objectMapper.createObjectNode();
        body.put("variableCode", "ODD_WORK_PROJECT_REMOTE");
        body.put("sourceSystem", "PRODUCE");
        body.put("remoteApi", "https://example.internal/odd-work/query");
        body.put("requestMethod", "POST");
        body.put("contentType", "application/json");
        body.put("requestHeadersJson", "{\"X-App\":\"cost-platform\"}");
        body.put("bodyTemplateJson", "{\"companyCode\":\"SG\",\"enabled\":true}");
        body.put("authType", "NONE");
        body.put("responseConfigJson", "{\"successPath\":\"status\",\"successValues\":[\"OK\"],\"messagePath\":\"message\",\"listPath\":\"data.items\"}");
        body.put("mappingConfigJson", "{\"sourceCode\":\"code\",\"sourceName\":\"name\",\"businessDomain\":\"company.code\",\"mappedValue\":\"price\"}");
        body.put("adapterType", "STANDARD");
        body.put("syncMode", "REALTIME");
        body.put("cachePolicy", "MANUAL_REFRESH");
        body.put("fallbackPolicy", "FAIL_FAST");

        JsonNode data = readData(mockMvc.perform(post("/cost/variable/remote/preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(data.path("success").asBoolean()).isTrue();
        assertThat(data.path("statusCode").asInt()).isEqualTo(200);
        assertThat(data.path("rawRows").isArray()).isTrue();
        assertThat(data.path("rawRows").path(0).path("sourceCode").asText()).isEqualTo("ODD-02");
        assertThat(data.path("mappedRows").path(0).path("sourceName").asText()).isEqualTo("零工乙");
        assertThat(data.path("mappedRows").path(0).path("businessDomain").asText()).isEqualTo("SG");
        assertThat(data.path("mappedRows").path(0).path("mappedValue").asText()).isEqualTo("18.8");
        server.verify();
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
