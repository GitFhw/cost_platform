package com.ruoyi.web.controller.cost;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.CostPlatformApplication;
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
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CostPlatformApplication.class)
@AutoConfigureMockMvc
class CostRunControllerManualIT
{
    private static final DateTimeFormatter STAMP_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisCache redisCache;

    @Test
    void shouldBuildFeeTemplateAndQueryFeeResultDetail() throws Exception
    {
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String billMonth = YearMonth.now().plusMonths(2).toString();

        JsonNode feeTemplate = readData(mockMvc.perform(get("/cost/run/input-template/fee")
                        .header("Authorization", authorization)
                        .param("sceneId", "10003")
                        .param("versionId", "3")
                        .param("feeCode", "STORAGE_KEEP_FEE")
                        .param("taskType", "FORMAL_BATCH"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(feeTemplate.path("fee").path("feeCode").asText()).isEqualTo("STORAGE_KEEP_FEE");
        assertThat(feeTemplate.path("inputFieldCount").asInt()).isEqualTo(3);
        assertThat(readIncludedFlags(feeTemplate.path("fields")))
                .containsEntry("CUSTOMER_LEVEL", true)
                .containsEntry("OCCUPIED_AREA", true)
                .containsEntry("BILL_DAYS", true)
                .containsEntry("AREA_DAYS", false);

        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        String requestNo = "REQ-" + stamp;
        String objectCode = "WH-" + stamp + "-001";
        ArrayNode inputItems = objectMapper.createArrayNode();
        inputItems.add(createInputItem("FORMAL-" + stamp + "-001", objectCode, "库区A", "A", 120, 20));
        inputItems.add(createInputItem("FORMAL-" + stamp + "-002", "WH-" + stamp + "-002", "库区C", "C", 80, 15));

        ObjectNode feeCalculateBody = objectMapper.createObjectNode();
        feeCalculateBody.put("sceneId", 10003);
        feeCalculateBody.put("versionId", 3);
        feeCalculateBody.put("feeCode", "STORAGE_KEEP_FEE");
        feeCalculateBody.put("billMonth", billMonth);
        feeCalculateBody.put("includeExplain", true);
        feeCalculateBody.put("inputJson", objectMapper.writeValueAsString(inputItems));

        JsonNode feeCalculate = readData(mockMvc.perform(post("/cost/run/fee/calculate")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(feeCalculateBody)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(feeCalculate.path("fee").path("feeCode").asText()).isEqualTo("STORAGE_KEEP_FEE");
        assertThat(feeCalculate.path("includeExplain").asBoolean()).isTrue();
        assertThat(feeCalculate.path("recordCount").asInt()).isEqualTo(2);
        assertThat(feeCalculate.path("successCount").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(feeCalculate.path("failedCount").asInt()).isEqualTo(0);
        assertThat(feeCalculate.path("records").get(0).path("feeCode").asText()).isEqualTo("STORAGE_KEEP_FEE");
        assertThat(feeCalculate.path("records").get(0).path("status").asText()).isIn("SUCCESS", "NO_MATCH");
        assertThat(feeCalculate.path("records").get(0).path("durationMs").asLong()).isGreaterThanOrEqualTo(0L);
        assertThat(feeCalculate.path("records").get(0).path("explain").path("timeline").isMissingNode()).isFalse();
        assertThat(feeCalculate.path("records").get(1).path("status").asText()).isEqualTo("NO_MATCH");
        assertThat(feeCalculate.path("records").get(1).path("explain").path("matched").asBoolean()).isFalse();
        assertThat(feeCalculate.path("records").get(1).path("explain").path("candidateRules").isArray()).isTrue();

        ObjectNode taskBody = objectMapper.createObjectNode();
        taskBody.put("sceneId", 10003);
        taskBody.put("versionId", 3);
        taskBody.put("taskType", "FORMAL_BATCH");
        taskBody.put("billMonth", billMonth);
        taskBody.put("requestNo", requestNo);
        taskBody.put("inputJson", objectMapper.writeValueAsString(inputItems));

        JsonNode taskSubmit = readData(mockMvc.perform(post("/cost/run/task/submit")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(taskBody)))
                .andExpect(status().isOk())
                .andReturn());

        long taskId = taskSubmit.path("task").path("taskId").asLong();
        String taskStatus = waitTaskFinished(taskId, authorization);
        assertThat(taskStatus).isIn("SUCCESS", "PART_SUCCESS");

        JsonNode taskList = readBody(mockMvc.perform(get("/cost/run/task/list")
                        .header("Authorization", authorization)
                        .param("requestNo", requestNo)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(taskList.path("total").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(taskList.path("rows").get(0).path("requestNo").asText()).isEqualTo(requestNo);

        JsonNode resultList = readBody(mockMvc.perform(get("/cost/run/result/list")
                        .header("Authorization", authorization)
                        .param("requestNo", requestNo)
                        .param("feeCode", "STORAGE_KEEP_FEE")
                        .param("objectCode", objectCode)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(resultList.path("total").asInt()).isGreaterThanOrEqualTo(1);
        JsonNode firstRow = resultList.path("rows").get(0);
        assertThat(firstRow.path("feeCode").asText()).isEqualTo("STORAGE_KEEP_FEE");
        assertThat(firstRow.path("objectCode").asText()).isEqualTo(objectCode);

        JsonNode resultDetail = readData(mockMvc.perform(get("/cost/run/result/{resultId}", firstRow.path("resultId").asLong())
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(resultDetail.path("ledger").path("feeCode").asText()).isEqualTo("STORAGE_KEEP_FEE");
        assertThat(resultDetail.path("trace").isMissingNode()).isFalse();
        assertThat(resultDetail.path("trace").path("pricing").isMissingNode()).isFalse();
    }

    private ObjectNode createInputItem(String bizNo, String objectCode, String objectName,
                                       String customerLevel, int occupiedArea, int billDays)
    {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("bizNo", bizNo);
        item.put("objectCode", objectCode);
        item.put("objectName", objectName);
        item.put("CUSTOMER_LEVEL", customerLevel);
        item.put("OCCUPIED_AREA", occupiedArea);
        item.put("BILL_DAYS", billDays);
        return item;
    }

    private String waitTaskFinished(long taskId, String authorization) throws Exception
    {
        long deadline = System.currentTimeMillis() + 60_000L;
        String taskStatus = "";
        while (System.currentTimeMillis() < deadline)
        {
            JsonNode taskDetail = readData(mockMvc.perform(get("/cost/run/task/{taskId}", taskId)
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            taskStatus = taskDetail.path("task").path("taskStatus").asText();
            if (!"INIT".equals(taskStatus) && !"RUNNING".equals(taskStatus))
            {
                return taskStatus;
            }
            Thread.sleep(1_000L);
        }
        return taskStatus;
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

    private Map<String, Boolean> readIncludedFlags(JsonNode fields)
    {
        Map<String, Boolean> result = new HashMap<>();
        Iterator<JsonNode> iterator = fields.iterator();
        while (iterator.hasNext())
        {
            JsonNode field = iterator.next();
            result.put(field.path("variableCode").asText(), field.path("includedInTemplate").asBoolean());
        }
        return result;
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
