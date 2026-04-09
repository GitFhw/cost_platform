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
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CostAccessProfileControllerManualIT {
    private static final long SCENE_ID = 10005L;
    private static final long FEE_ID = 20041L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private RestTemplate costAccessRestTemplate;

    @Test
    void shouldCreateUpdateAndRemoveAccessProfile() throws Exception {
        String token = loginAndGetToken();
        String profileCode = "IT_ACCESS_PROFILE_" + System.currentTimeMillis();

        ObjectNode addBody = objectMapper.createObjectNode();
        addBody.put("sceneId", SCENE_ID);
        addBody.put("feeId", FEE_ID);
        addBody.put("profileCode", profileCode);
        addBody.put("profileName", "接入方案联调样例");
        addBody.put("sourceType", "RAW_JSON");
        addBody.put("taskType", "FORMAL_BATCH");
        addBody.put("requestMethod", "POST");
        addBody.put("endpointUrl", "https://example.internal/report");
        addBody.put("authType", "NONE");
        addBody.put("mappingJson", "{\"bizNo\":\"meta.bizNo\"}");
        addBody.put("samplePayloadJson", "[{\"meta\":{\"bizNo\":\"A-001\"}}]");
        addBody.put("sampleInputJson", "[{\"bizNo\":\"A-001\",\"objectCode\":\"TEAM-01\",\"objectName\":\"一队\"}]");
        addBody.put("status", "0");

        JsonNode add = readBody(mockMvc.perform(post("/cost/access/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(addBody)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(add.path("code").asInt()).isEqualTo(200);

        JsonNode options = readData(mockMvc.perform(get("/cost/access/profile/options")
                        .header("Authorization", "Bearer " + token)
                        .param("sceneId", String.valueOf(SCENE_ID))
                        .param("feeId", String.valueOf(FEE_ID)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode profileNode = findNodeByField(options, "profileCode", profileCode);
        assertThat(profileNode).isNotNull();
        Long profileId = profileNode.path("profileId").asLong();

        JsonNode detail = readData(mockMvc.perform(get("/cost/access/profile/" + profileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(detail.path("profileName").asText()).isEqualTo("接入方案联调样例");
        assertThat(detail.path("feeCode").asText()).isEqualTo("SG_FEMALE_SHIFT_LABOR");

        ObjectNode editBody = (ObjectNode) detail.deepCopy();
        editBody.put("profileName", "接入方案联调样例-更新");
        editBody.put("remark", "updated");
        JsonNode edit = readBody(mockMvc.perform(put("/cost/access/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(editBody)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(edit.path("code").asInt()).isEqualTo(200);

        JsonNode updated = readData(mockMvc.perform(get("/cost/access/profile/" + profileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(updated.path("profileName").asText()).isEqualTo("接入方案联调样例-更新");
        assertThat(updated.path("remark").asText()).isEqualTo("updated");

        JsonNode remove = readBody(mockMvc.perform(delete("/cost/access/profile/" + profileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(remove.path("code").asInt()).isEqualTo(200);

        JsonNode optionsAfterRemove = readData(mockMvc.perform(get("/cost/access/profile/options")
                        .header("Authorization", "Bearer " + token)
                        .param("sceneId", String.valueOf(SCENE_ID))
                        .param("feeId", String.valueOf(FEE_ID)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(findNodeByField(optionsAfterRemove, "profileCode", profileCode)).isNull();
    }

    @Test
    void shouldFetchHttpProfileAndPreviewBuiltInput() throws Exception {
        String token = loginAndGetToken();
        String profileCode = "IT_HTTP_PROFILE_" + System.currentTimeMillis();

        ObjectNode addBody = objectMapper.createObjectNode();
        addBody.put("sceneId", SCENE_ID);
        addBody.put("feeId", FEE_ID);
        addBody.put("profileCode", profileCode);
        addBody.put("profileName", "HTTP直连预演联调样例");
        addBody.put("sourceType", "HTTP_API");
        addBody.put("taskType", "FORMAL_BATCH");
        addBody.put("requestMethod", "POST");
        addBody.put("endpointUrl", "https://example.internal/report");
        addBody.put("authType", "BEARER");
        addBody.put("authConfigJson", "{\"token\":\"demo-token\",\"headers\":{\"X-App\":\"cost-platform\"}}");
        addBody.put("mappingJson", "{\"bizNo\":\"payload.bizNo\",\"objectCode\":\"payload.teamCode\",\"objectName\":\"payload.teamName\",\"FEMALE_TEAM_HEADCOUNT\":\"payload.FEMALE_TEAM_HEADCOUNT\"}");
        addBody.put("samplePayloadJson", "{\"billMonth\":\"2026-04\"}");
        addBody.put("status", "0");

        JsonNode add = readBody(mockMvc.perform(post("/cost/access/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(addBody)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(add.path("code").asInt()).isEqualTo(200);

        JsonNode options = readData(mockMvc.perform(get("/cost/access/profile/options")
                        .header("Authorization", "Bearer " + token)
                        .param("sceneId", String.valueOf(SCENE_ID))
                        .param("feeId", String.valueOf(FEE_ID)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode profileNode = findNodeByField(options, "profileCode", profileCode);
        assertThat(profileNode).isNotNull();
        long profileId = profileNode.path("profileId").asLong();

        MockRestServiceServer server = MockRestServiceServer.bindTo(costAccessRestTemplate).build();
        server.expect(once(), requestTo("https://example.internal/report"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer demo-token"))
                .andExpect(header("X-App", "cost-platform"))
                .andExpect(content().json("{\"billMonth\":\"2026-04\"}"))
                .andRespond(withSuccess("[{\"payload\":{\"bizNo\":\"HTTP-001\",\"teamCode\":\"TEAM-01\",\"teamName\":\"测试协力队\",\"FEMALE_TEAM_HEADCOUNT\":2}}]",
                        MediaType.APPLICATION_JSON));

        try {
            JsonNode preview = readData(mockMvc.perform(post("/cost/access/profile/" + profileId + "/preview-fetch")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"requestPayloadJson\":\"{\\\"billMonth\\\":\\\"2026-04\\\"}\"}"))
                    .andExpect(status().isOk())
                    .andReturn());

            assertThat(preview.path("fetchMeta").path("statusCode").asInt()).isEqualTo(200);
            assertThat(preview.path("accessProfile").path("profileCode").asText()).isEqualTo(profileCode);
            assertThat(preview.path("mappedRecordCount").asInt()).isEqualTo(1);
            assertThat(preview.path("mappedRecords").get(0).path("bizNo").asText()).isEqualTo("HTTP-001");
            assertThat(preview.path("mappedRecords").get(0).path("objectCode").asText()).isEqualTo("TEAM-01");
            assertThat(preview.path("fetchedPayloadJson").isArray()).isTrue();
        } finally {
            server.verify();
            mockMvc.perform(delete("/cost/access/profile/" + profileId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void shouldFetchHttpProfileAndCreateInputBatch() throws Exception {
        String token = loginAndGetToken();
        String profileCode = "IT_HTTP_BATCH_" + System.currentTimeMillis();

        ObjectNode addBody = objectMapper.createObjectNode();
        addBody.put("sceneId", SCENE_ID);
        addBody.put("feeId", FEE_ID);
        addBody.put("profileCode", profileCode);
        addBody.put("profileName", "HTTP直连批次联调样例");
        addBody.put("sourceType", "HTTP_API");
        addBody.put("taskType", "FORMAL_BATCH");
        addBody.put("requestMethod", "POST");
        addBody.put("endpointUrl", "https://example.internal/report-batch");
        addBody.put("authType", "BEARER");
        addBody.put("authConfigJson", "{\"token\":\"demo-token\"}");
        addBody.put("mappingJson", "{\"bizNo\":\"payload.bizNo\",\"objectCode\":\"payload.teamCode\",\"objectName\":\"payload.teamName\",\"FEMALE_TEAM_HEADCOUNT\":\"payload.FEMALE_TEAM_HEADCOUNT\"}");
        addBody.put("samplePayloadJson", "{\"billMonth\":\"2026-05\"}");
        addBody.put("status", "0");

        JsonNode add = readBody(mockMvc.perform(post("/cost/access/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(addBody)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(add.path("code").asInt()).isEqualTo(200);

        JsonNode options = readData(mockMvc.perform(get("/cost/access/profile/options")
                        .header("Authorization", "Bearer " + token)
                        .param("sceneId", String.valueOf(SCENE_ID))
                        .param("feeId", String.valueOf(FEE_ID)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode profileNode = findNodeByField(options, "profileCode", profileCode);
        assertThat(profileNode).isNotNull();
        long profileId = profileNode.path("profileId").asLong();

        MockRestServiceServer server = MockRestServiceServer.bindTo(costAccessRestTemplate).build();
        server.expect(once(), requestTo("https://example.internal/report-batch"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer demo-token"))
                .andRespond(withSuccess("[{\"payload\":{\"bizNo\":\"BATCH-001\",\"teamCode\":\"TEAM-BATCH-01\",\"teamName\":\"批次协力队\",\"FEMALE_TEAM_HEADCOUNT\":3}}]",
                        MediaType.APPLICATION_JSON));

        try {
            JsonNode batch = readData(mockMvc.perform(post("/cost/access/profile/" + profileId + "/input-batch")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"billMonth\":\"2026-05\"}"))
                    .andExpect(status().isOk())
                    .andReturn());

            assertThat(batch.path("batch").path("batchNo").asText()).isNotBlank();
            assertThat(batch.path("batch").path("totalCount").asInt()).isEqualTo(1);
            assertThat(batch.path("itemTotal").asInt()).isEqualTo(1);
            assertThat(batch.path("fetchMeta").path("statusCode").asInt()).isEqualTo(200);
            assertThat(batch.path("mappedRecordCount").asInt()).isEqualTo(1);
            assertThat(batch.path("mappedRecords").get(0).path("objectCode").asText()).isEqualTo("TEAM-BATCH-01");
        } finally {
            server.verify();
            mockMvc.perform(delete("/cost/access/profile/" + profileId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void shouldFetchPagedHttpProfileAndCreateInputBatch() throws Exception {
        String token = loginAndGetToken();
        String profileCode = "IT_HTTP_PAGED_BATCH_" + System.currentTimeMillis();

        ObjectNode addBody = objectMapper.createObjectNode();
        addBody.put("sceneId", SCENE_ID);
        addBody.put("feeId", FEE_ID);
        addBody.put("profileCode", profileCode);
        addBody.put("profileName", "HTTP分页批次联调样例");
        addBody.put("sourceType", "HTTP_API");
        addBody.put("taskType", "FORMAL_BATCH");
        addBody.put("requestMethod", "GET");
        addBody.put("endpointUrl", "https://example.internal/paged-report");
        addBody.put("authType", "NONE");
        addBody.put("fetchConfigJson", "{\"recordsPath\":\"data.records\",\"paging\":{\"mode\":\"PAGE_NO\",\"pageField\":\"pageNo\",\"pageSizeField\":\"pageSize\",\"pageSize\":2,\"startPage\":1,\"maxPages\":5,\"hasMorePath\":\"data.hasMore\"}}");
        addBody.put("mappingJson", "{\"bizNo\":\"payload.bizNo\",\"objectCode\":\"payload.teamCode\",\"objectName\":\"payload.teamName\",\"FEMALE_TEAM_HEADCOUNT\":\"payload.FEMALE_TEAM_HEADCOUNT\"}");
        addBody.put("samplePayloadJson", "{\"billMonth\":\"2026-06\",\"tenantCode\":\"TENANT-A\"}");
        addBody.put("status", "0");

        JsonNode add = readBody(mockMvc.perform(post("/cost/access/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(addBody)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(add.path("code").asInt()).isEqualTo(200);

        JsonNode options = readData(mockMvc.perform(get("/cost/access/profile/options")
                        .header("Authorization", "Bearer " + token)
                        .param("sceneId", String.valueOf(SCENE_ID))
                        .param("feeId", String.valueOf(FEE_ID)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode profileNode = findNodeByField(options, "profileCode", profileCode);
        assertThat(profileNode).isNotNull();
        long profileId = profileNode.path("profileId").asLong();

        MockRestServiceServer server = MockRestServiceServer.bindTo(costAccessRestTemplate).build();
        server.expect(once(), requestTo("https://example.internal/paged-report?billMonth=2026-06&tenantCode=TENANT-A&pageNo=1&pageSize=2"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("{\"data\":{\"records\":[{\"payload\":{\"bizNo\":\"PAGE-001\",\"teamCode\":\"TEAM-01\",\"teamName\":\"协力队一\",\"FEMALE_TEAM_HEADCOUNT\":2}},{\"payload\":{\"bizNo\":\"PAGE-002\",\"teamCode\":\"TEAM-02\",\"teamName\":\"协力队二\",\"FEMALE_TEAM_HEADCOUNT\":3}}],\"hasMore\":true}}",
                        MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://example.internal/paged-report?billMonth=2026-06&tenantCode=TENANT-A&pageNo=2&pageSize=2"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("{\"data\":{\"records\":[{\"payload\":{\"bizNo\":\"PAGE-003\",\"teamCode\":\"TEAM-03\",\"teamName\":\"协力队三\",\"FEMALE_TEAM_HEADCOUNT\":4}}],\"hasMore\":false}}",
                        MediaType.APPLICATION_JSON));

        try {
            JsonNode batch = readData(mockMvc.perform(post("/cost/access/profile/" + profileId + "/input-batch")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"billMonth\":\"2026-06\"}"))
                    .andExpect(status().isOk())
                    .andReturn());

            assertThat(batch.path("batch").path("batchNo").asText()).isNotBlank();
            assertThat(batch.path("batch").path("totalCount").asInt()).isEqualTo(3);
            assertThat(batch.path("itemTotal").asInt()).isEqualTo(3);
            assertThat(batch.path("fetchMeta").path("paged").asBoolean()).isTrue();
            assertThat(batch.path("fetchMeta").path("pageCount").asInt()).isEqualTo(2);
            assertThat(batch.path("fetchMeta").path("recordCount").asInt()).isEqualTo(3);
            assertThat(batch.path("mappedRecordCount").asInt()).isEqualTo(3);
            assertThat(batch.path("mappedRecords").get(0).path("objectCode").asText()).isEqualTo("TEAM-01");
        } finally {
            server.verify();
            mockMvc.perform(delete("/cost/access/profile/" + profileId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void shouldResumePagedHttpProfileInputBatch() throws Exception {
        String token = loginAndGetToken();
        String profileCode = "IT_HTTP_RESUME_BATCH_" + System.currentTimeMillis();

        ObjectNode addBody = objectMapper.createObjectNode();
        addBody.put("sceneId", SCENE_ID);
        addBody.put("feeId", FEE_ID);
        addBody.put("profileCode", profileCode);
        addBody.put("profileName", "HTTP分页续拉联调样例");
        addBody.put("sourceType", "HTTP_API");
        addBody.put("taskType", "FORMAL_BATCH");
        addBody.put("requestMethod", "GET");
        addBody.put("endpointUrl", "https://example.internal/paged-resume");
        addBody.put("authType", "NONE");
        addBody.put("fetchConfigJson", "{\"recordsPath\":\"data.records\",\"paging\":{\"mode\":\"PAGE_NO\",\"pageField\":\"pageNo\",\"pageSizeField\":\"pageSize\",\"pageSize\":2,\"startPage\":1,\"maxPages\":1,\"hasMorePath\":\"data.hasMore\"}}");
        addBody.put("mappingJson", "{\"bizNo\":\"payload.bizNo\",\"objectCode\":\"payload.teamCode\",\"objectName\":\"payload.teamName\",\"FEMALE_TEAM_HEADCOUNT\":\"payload.FEMALE_TEAM_HEADCOUNT\"}");
        addBody.put("samplePayloadJson", "{\"billMonth\":\"2026-07\",\"tenantCode\":\"TENANT-B\"}");
        addBody.put("status", "0");

        JsonNode add = readBody(mockMvc.perform(post("/cost/access/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(addBody)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(add.path("code").asInt()).isEqualTo(200);

        JsonNode options = readData(mockMvc.perform(get("/cost/access/profile/options")
                        .header("Authorization", "Bearer " + token)
                        .param("sceneId", String.valueOf(SCENE_ID))
                        .param("feeId", String.valueOf(FEE_ID)))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode profileNode = findNodeByField(options, "profileCode", profileCode);
        assertThat(profileNode).isNotNull();
        long profileId = profileNode.path("profileId").asLong();

        MockRestServiceServer server = MockRestServiceServer.bindTo(costAccessRestTemplate).build();
        server.expect(once(), requestTo("https://example.internal/paged-resume?billMonth=2026-07&tenantCode=TENANT-B&pageNo=1&pageSize=2"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("{\"data\":{\"records\":[{\"payload\":{\"bizNo\":\"RESUME-001\",\"teamCode\":\"TEAM-11\",\"teamName\":\"协力队一\",\"FEMALE_TEAM_HEADCOUNT\":2}},{\"payload\":{\"bizNo\":\"RESUME-002\",\"teamCode\":\"TEAM-12\",\"teamName\":\"协力队二\",\"FEMALE_TEAM_HEADCOUNT\":3}}],\"hasMore\":true}}",
                        MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://example.internal/paged-resume?billMonth=2026-07&tenantCode=TENANT-B&pageNo=2&pageSize=2"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("{\"data\":{\"records\":[{\"payload\":{\"bizNo\":\"RESUME-003\",\"teamCode\":\"TEAM-13\",\"teamName\":\"协力队三\",\"FEMALE_TEAM_HEADCOUNT\":4}}],\"hasMore\":false}}",
                        MediaType.APPLICATION_JSON));

        try {
            JsonNode firstBatch = readData(mockMvc.perform(post("/cost/access/profile/" + profileId + "/input-batch")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"billMonth\":\"2026-07\"}"))
                    .andExpect(status().isOk())
                    .andReturn());

            assertThat(firstBatch.path("batch").path("batchStatus").asText()).isEqualTo("PARTIAL");
            assertThat(firstBatch.path("batch").path("totalCount").asInt()).isEqualTo(2);
            assertThat(firstBatch.path("resumable").asBoolean()).isTrue();
            assertThat(firstBatch.path("checkpoint").path("nextPageNo").asInt()).isEqualTo(2);

            JsonNode resumedBatch = readData(mockMvc.perform(post("/cost/access/profile/" + profileId + "/input-batch")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"resumeBatchId\":" + firstBatch.path("batch").path("batchId").asLong() + "}"))
                    .andExpect(status().isOk())
                    .andReturn());

            assertThat(resumedBatch.path("resumed").asBoolean()).isTrue();
            assertThat(resumedBatch.path("resumable").asBoolean()).isFalse();
            assertThat(resumedBatch.path("batch").path("batchStatus").asText()).isEqualTo("READY");
            assertThat(resumedBatch.path("batch").path("totalCount").asInt()).isEqualTo(3);
            assertThat(resumedBatch.path("itemTotal").asInt()).isEqualTo(3);
            assertThat(resumedBatch.path("checkpoint").path("hasMore").asBoolean()).isFalse();
            assertThat(resumedBatch.path("mappedRecords").get(0).path("objectCode").asText()).isEqualTo("TEAM-13");
        } finally {
            server.verify();
            mockMvc.perform(delete("/cost/access/profile/" + profileId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
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
