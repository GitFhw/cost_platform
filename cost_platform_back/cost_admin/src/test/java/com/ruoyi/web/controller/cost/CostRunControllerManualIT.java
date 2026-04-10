package com.ruoyi.web.controller.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.config.cost.CostDispatchProperties;
import com.ruoyi.system.domain.cost.*;
import com.ruoyi.system.mapper.cost.*;
import com.ruoyi.system.service.impl.cost.CostDistributedLockSupport;
import com.ruoyi.system.service.impl.cost.CostRunServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CostRunControllerManualIT {
    private static final DateTimeFormatter STAMP_FORMATTER = DateTimeFormatter.ofPattern("HHmmssSSS");
    private static final String SCENE_CODE = "SHOUGANG-ORE-HR-001";
    private static final String FEMALE_FEE_CODE = "SG_FEMALE_SHIFT_LABOR";
    private static final String COVER_FEE_CODE = "SG_COVER_ODD_JOB_LABOR";
    private static final String MANAGEMENT_FEE_CODE = "SG_MANAGEMENT_FEE";
    private static final String MANAGEMENT_FEE_FORMULA_BASELINE =
            "round((coalesce(F['SG_THRPT_PIECE_FEE']?.pricing?.amountValue, 0) + "
                    + "coalesce(F['SG_FEMALE_SHIFT_LABOR']?.pricing?.amountValue, 0) + "
                    + "coalesce(F['SG_SPECIAL_SHIFT_LABOR']?.pricing?.amountValue, 0) + "
                    + "coalesce(F['SG_HOLD_CLEANING_LABOR']?.pricing?.amountValue, 0) + "
                    + "coalesce(F['SG_COVER_ODD_JOB_LABOR']?.pricing?.amountValue, 0) + "
                    + "coalesce(F['SG_MOORING_FEE']?.pricing?.amountValue, 0) + "
                    + "coalesce(F['SG_ODD_JOB_FEE']?.pricing?.amountValue, 0) + "
                    + "coalesce(F['SG_DUTY_SHIFT_LABOR']?.pricing?.amountValue, 0) + "
                    + "coalesce(F['SG_SEASONAL_ALLOWANCE']?.pricing?.amountValue, 0) + "
                    + "coalesce(F['SG_OVERTIME_FEE']?.pricing?.amountValue, 0)) * 0.1677, 2)";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private CostSceneMapper sceneMapper;

    @Autowired
    private CostPublishVersionMapper publishVersionMapper;

    @Autowired
    private CostRuleMapper ruleMapper;

    @Autowired
    private CostFormulaMapper formulaMapper;

    @Autowired
    private CostFeeMapper feeMapper;

    @Autowired
    private CostVariableMapper variableMapper;

    @Autowired
    private CostCalcTaskDetailMapper taskDetailMapper;

    @Autowired
    private CostCalcTaskMapper taskMapper;

    @Autowired
    private CostCalcTaskPartitionMapper taskPartitionMapper;

    @Autowired
    private CostDistributedLockSupport distributedLockSupport;

    @Autowired
    private CostRunServiceImpl costRunService;

    @Autowired
    private CostDispatchProperties costDispatchProperties;

    @Test
    void shouldUseDraftConfigWhenSimulationVersionIsNotSpecified() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "draft-simulation-default-" + stamp);
        CostScene scene = sceneMapper.selectById(sceneId);
        Long previousActiveVersionId = scene == null ? null : scene.getActiveVersionId();
        sceneMapper.update(null, Wrappers.<CostScene>lambdaUpdate()
                .eq(CostScene::getSceneId, sceneId)
                .set(CostScene::getActiveVersionId, versionId));

        try {
            JsonNode template = readData(mockMvc.perform(get("/cost/run/input-template")
                            .header("Authorization", authorization)
                            .param("sceneId", String.valueOf(sceneId))
                            .param("taskType", "SIMULATION"))
                    .andExpect(status().isOk())
                    .andReturn());

            assertThat(template.path("versionId").isNull()).isTrue();
            assertThat(template.path("versionNo").asText()).isEqualTo("\u8349\u7a3f\u914d\u7f6e");
            assertThat(template.path("snapshotSource").asText()).isEqualTo("DRAFT");

            ObjectNode executeBody = objectMapper.createObjectNode();
            executeBody.put("sceneId", sceneId);
            executeBody.put("inputJson", objectMapper.writeValueAsString(
                    createManagementInputItem("SG-DRAFT-BIZ-" + stamp, "SG-DRAFT-" + stamp, "draft-simulation")));

            JsonNode simulation = readData(mockMvc.perform(post("/cost/run/simulation/execute")
                            .header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(executeBody)))
                    .andExpect(status().isOk())
                    .andReturn());

            assertThat(simulation.path("record").path("status").asText()).isEqualTo("SUCCESS");
            assertThat(simulation.path("record").path("versionId").isNull()).isTrue();
            assertThat(simulation.path("record").path("versionNo").asText()).isEqualTo("\u8349\u7a3f\u914d\u7f6e");
            assertThat(simulation.path("result").path("versionNo").asText()).isEqualTo("\u8349\u7a3f\u914d\u7f6e");
            assertThat(simulation.path("result").path("snapshotSource").asText()).isEqualTo("DRAFT");
        } finally {
            sceneMapper.update(null, Wrappers.<CostScene>lambdaUpdate()
                    .eq(CostScene::getSceneId, sceneId)
                    .set(CostScene::getActiveVersionId, previousActiveVersionId));
        }
    }

    @Test
    void shouldKeepPublishedSnapshotStableAfterDraftRuleChanges() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "snapshot-stability-" + stamp);

        CostRule femaleRule = ruleMapper.selectOne(Wrappers.<CostRule>lambdaQuery()
                .eq(CostRule::getSceneId, sceneId)
                .eq(CostRule::getRuleCode, "SG_FEMALE_SHIFT_RATE_01"));
        assertThat(femaleRule).isNotNull();
        String previousPricingJson = femaleRule.getPricingJson();
        femaleRule.setPricingJson("{\"mode\":\"FIXED_RATE\",\"basis\":\"FEMALE_ATTENDANCE_EQUIV\",\"unit\":\"UNIT\",\"rateValue\":4000,\"summary\":\"draft regression price\"}");
        ruleMapper.updateById(femaleRule);

        try {
            ObjectNode input = createManagementInputItem("SG-SNAPSHOT-BIZ-" + stamp, "SG-SNAPSHOT-" + stamp, "snapshot-regression");

            ObjectNode publishedBody = objectMapper.createObjectNode();
            publishedBody.put("sceneId", sceneId);
            publishedBody.put("versionId", versionId);
            publishedBody.put("inputJson", objectMapper.writeValueAsString(input));

            JsonNode publishedSimulation = readData(mockMvc.perform(post("/cost/run/simulation/execute")
                            .header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(publishedBody)))
                    .andExpect(status().isOk())
                    .andReturn());

            assertThat(publishedSimulation.path("result").path("snapshotSource").asText()).isEqualTo("PUBLISHED");
            JsonNode publishedFemaleFee = findNodeByField(publishedSimulation.path("result").path("feeResults"), "feeCode", FEMALE_FEE_CODE);
            assertThat(publishedFemaleFee).isNotNull();
            assertThat(publishedFemaleFee.path("amountValue").decimalValue()).isEqualByComparingTo("7233.33");

            ObjectNode draftBody = objectMapper.createObjectNode();
            draftBody.put("sceneId", sceneId);
            draftBody.put("inputJson", objectMapper.writeValueAsString(input));

            JsonNode draftSimulation = readData(mockMvc.perform(post("/cost/run/simulation/execute")
                            .header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(draftBody)))
                    .andExpect(status().isOk())
                    .andReturn());

            assertThat(draftSimulation.path("result").path("snapshotSource").asText()).isEqualTo("DRAFT");
            JsonNode draftFemaleFee = findNodeByField(draftSimulation.path("result").path("feeResults"), "feeCode", FEMALE_FEE_CODE);
            assertThat(draftFemaleFee).isNotNull();
            assertThat(draftFemaleFee.path("amountValue").decimalValue()).isEqualByComparingTo("8000.00");

            String requestNo = "SG-SNAPSHOT-REQ-" + stamp;
            ObjectNode taskBody = objectMapper.createObjectNode();
            taskBody.put("sceneId", sceneId);
            taskBody.put("versionId", versionId);
            taskBody.put("taskType", "FORMAL_BATCH");
            taskBody.put("billMonth", YearMonth.now().plusMonths(2).toString());
            taskBody.put("requestNo", requestNo);
            taskBody.put("inputJson", objectMapper.writeValueAsString(objectMapper.createArrayNode().add(input)));

            JsonNode taskSubmit = readData(mockMvc.perform(post("/cost/run/task/submit")
                            .header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(taskBody)))
                    .andExpect(status().isOk())
                    .andReturn());

            long taskId = taskSubmit.path("task").path("taskId").asLong();
            assertThat(waitTaskFinished(taskId, authorization)).isIn("SUCCESS", "PART_SUCCESS");

            JsonNode resultList = readBody(mockMvc.perform(get("/cost/run/result/list")
                            .header("Authorization", authorization)
                            .param("requestNo", requestNo)
                            .param("feeCode", FEMALE_FEE_CODE)
                            .param("objectCode", input.path("objectCode").asText())
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andReturn());

            JsonNode femaleLedger = findNodeByField(resultList.path("rows"), "feeCode", FEMALE_FEE_CODE);
            assertThat(femaleLedger).isNotNull();
            assertThat(femaleLedger.path("amountValue").decimalValue()).isEqualByComparingTo("7233.33");
        } finally {
            femaleRule.setPricingJson(previousPricingJson);
            ruleMapper.updateById(femaleRule);
        }
    }

    @Test
    void shouldSubmitFormalTaskFromInputBatch() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "input-batch-formal-" + stamp);
        String billMonth = YearMonth.now().plusMonths(2).toString();

        ArrayNode inputItems = objectMapper.createArrayNode();
        inputItems.add(createFemaleInputItem("SG-BATCH-BIZ-" + stamp + "-001", "SG-BATCH-" + stamp + "-A", "batch-female-a", 2, 6, 6));
        inputItems.add(createFemaleInputItem("SG-BATCH-BIZ-" + stamp + "-002", "SG-BATCH-" + stamp + "-B", "batch-female-b", 1, 0, 1));

        ObjectNode batchBody = objectMapper.createObjectNode();
        batchBody.put("sceneId", sceneId);
        batchBody.put("versionId", versionId);
        batchBody.put("billMonth", billMonth);
        batchBody.put("inputJson", objectMapper.writeValueAsString(inputItems));
        batchBody.put("remark", "input-batch-regression");

        JsonNode batchDetail = readData(mockMvc.perform(post("/cost/run/task/input-batch")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(batchBody)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(batchDetail.path("batch").path("versionId").asLong()).isEqualTo(versionId);
        assertThat(batchDetail.path("batch").path("batchStatus").asText()).isEqualTo("READY");
        assertThat(batchDetail.path("items").size()).isEqualTo(2);
        assertThat(batchDetail.path("itemTotal").asInt()).isEqualTo(2);
        assertThat(batchDetail.path("loadingGuide").path("estimatedPartitionCount").asInt()).isEqualTo(1);
        assertThat(batchDetail.path("loadingGuide").path("partitionSize").asInt()).isEqualTo(500);
        String batchNo = batchDetail.path("batch").path("batchNo").asText();

        JsonNode batchDetailPageTwo = readData(mockMvc.perform(get("/cost/run/task/input-batch/{batchId}", batchDetail.path("batch").path("batchId").asLong())
                        .header("Authorization", authorization)
                        .param("pageNum", "2")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(batchDetailPageTwo.path("items").size()).isEqualTo(1);
        assertThat(batchDetailPageTwo.path("itemTotal").asInt()).isEqualTo(2);
        assertThat(batchDetailPageTwo.path("items").get(0).path("itemNo").asInt()).isEqualTo(2);

        ObjectNode taskBody = objectMapper.createObjectNode();
        taskBody.put("sceneId", sceneId);
        taskBody.put("versionId", versionId);
        taskBody.put("taskType", "FORMAL_BATCH");
        taskBody.put("billMonth", billMonth);
        taskBody.put("requestNo", "SG-BATCH-REQ-" + stamp);
        taskBody.put("inputSourceType", "INPUT_BATCH");
        taskBody.put("sourceBatchNo", batchNo);

        JsonNode taskDetail = readData(mockMvc.perform(post("/cost/run/task/submit")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(taskBody)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(taskDetail.path("task").path("sourceBatchNo").asText()).isEqualTo(batchNo);
        assertThat(taskDetail.path("task").path("inputSourceType").asText()).isEqualTo("INPUT_BATCH");
        assertThat(taskDetail.path("inputBatch").path("batch").path("batchStatus").asText()).isEqualTo("SUBMITTED");
        assertThat(taskDetail.path("inputBatch").path("items").path(0).path("itemStatus").asText()).isEqualTo("CONSUMED");

        long taskId = taskDetail.path("task").path("taskId").asLong();
        assertThat(waitTaskFinished(taskId, authorization)).isIn("SUCCESS", "PART_SUCCESS");

        JsonNode resultList = readBody(mockMvc.perform(get("/cost/run/result/list")
                        .header("Authorization", authorization)
                        .param("requestNo", taskDetail.path("task").path("requestNo").asText())
                        .param("feeCode", FEMALE_FEE_CODE)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(resultList.path("total").asInt()).isGreaterThanOrEqualTo(2);
        JsonNode ledgerA = findNodeByField(resultList.path("rows"), "objectCode", "SG-BATCH-" + stamp + "-A");
        JsonNode ledgerB = findNodeByField(resultList.path("rows"), "objectCode", "SG-BATCH-" + stamp + "-B");
        assertThat(ledgerA).isNotNull();
        assertThat(ledgerB).isNotNull();
        assertThat(ledgerA.path("amountValue").decimalValue()).isEqualByComparingTo("7233.33");
        assertThat(ledgerB.path("amountValue").decimalValue()).isEqualByComparingTo("0.00");

        JsonNode refreshedBatch = readData(mockMvc.perform(get("/cost/run/task/input-batch/{batchId}", batchDetail.path("batch").path("batchId").asLong())
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(refreshedBatch.path("batch").path("batchStatus").asText()).isEqualTo("SUBMITTED");
        assertThat(refreshedBatch.path("items").path(0).path("itemStatus").asText()).isEqualTo("CONSUMED");
    }

    @Test
    void shouldBlockFormalTaskSubmitWhenSubmitLockExists() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "task-lock-baseline-" + stamp);
        String billMonth = YearMonth.now().plusMonths(2).toString();
        String requestNo = "SG-LOCK-REQ-" + stamp;
        String lockKey = distributedLockSupport.buildTaskSubmitLockKey(sceneId, versionId, billMonth,
                "FORMAL_BATCH", requestNo, "");
        redisCache.setCacheObject(lockKey, "itest-lock", 30, TimeUnit.SECONDS);

        try {
            ObjectNode taskBody = objectMapper.createObjectNode();
            taskBody.put("sceneId", sceneId);
            taskBody.put("versionId", versionId);
            taskBody.put("taskType", "FORMAL_BATCH");
            taskBody.put("billMonth", billMonth);
            taskBody.put("requestNo", requestNo);
            taskBody.put("inputJson", objectMapper.writeValueAsString(objectMapper.createArrayNode()
                    .add(createManagementInputItem("SG-LOCK-BIZ-" + stamp, "SG-LOCK-" + stamp, "task-lock"))));

            JsonNode taskResponse = readBody(mockMvc.perform(post("/cost/run/task/submit")
                            .header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(taskBody)))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(taskResponse.path("code").asInt()).isEqualTo(500);
            assertThat(taskResponse.path("msg").asText()).contains("核算任务");
        } finally {
            redisCache.deleteObject(lockKey);
        }
    }

    @Test
    void shouldCreateLargeInputBatchWithChunkedInsertAndPagedPreview() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "large-input-batch-" + stamp);
        String billMonth = YearMonth.now().plusMonths(2).toString();

        ArrayNode inputItems = objectMapper.createArrayNode();
        for (int i = 1; i <= 520; i++) {
            inputItems.add(createFemaleInputItem(
                    String.format("SG-LARGE-BATCH-BIZ-%s-%03d", stamp, i),
                    String.format("SG-LARGE-BATCH-%s-%03d", stamp, i),
                    String.format("large-batch-%03d", i),
                    2,
                    6,
                    6));
        }

        ObjectNode batchBody = objectMapper.createObjectNode();
        batchBody.put("sceneId", sceneId);
        batchBody.put("versionId", versionId);
        batchBody.put("billMonth", billMonth);
        batchBody.put("inputJson", objectMapper.writeValueAsString(inputItems));
        batchBody.put("remark", "large-input-batch-regression");

        JsonNode batchDetail = readData(mockMvc.perform(post("/cost/run/task/input-batch")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(batchBody)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(batchDetail.path("batch").path("totalCount").asInt()).isEqualTo(520);
        assertThat(batchDetail.path("itemTotal").asInt()).isEqualTo(520);
        assertThat(batchDetail.path("items").size()).isEqualTo(10);
        assertThat(batchDetail.path("loadingGuide").path("estimatedPartitionCount").asInt()).isEqualTo(2);
        assertThat(batchDetail.path("loadingGuide").path("partitionSize").asInt()).isEqualTo(500);

        JsonNode secondPage = readData(mockMvc.perform(get("/cost/run/task/input-batch/{batchId}", batchDetail.path("batch").path("batchId").asLong())
                        .header("Authorization", authorization)
                        .param("pageNum", "2")
                        .param("pageSize", "200"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(secondPage.path("items").size()).isEqualTo(200);
        assertThat(secondPage.path("items").get(0).path("itemNo").asInt()).isEqualTo(201);

        JsonNode thirdPage = readData(mockMvc.perform(get("/cost/run/task/input-batch/{batchId}", batchDetail.path("batch").path("batchId").asLong())
                        .header("Authorization", authorization)
                        .param("pageNum", "3")
                        .param("pageSize", "200"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(thirdPage.path("items").size()).isEqualTo(120);
        assertThat(thirdPage.path("items").get(0).path("itemNo").asInt()).isEqualTo(401);
    }

    @Test
    void shouldCreateMultiplePartitionsForLargeFormalBatch() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "multi-partition-regression-" + stamp);
        String requestNo = "SG-MULTI-REQ-" + stamp;
        String billMonth = YearMonth.now().plusMonths(2).toString();

        ArrayNode inputItems = objectMapper.createArrayNode();
        for (int i = 1; i <= 501; i++) {
            inputItems.add(createFemaleInputItem(
                    String.format("SG-MULTI-BIZ-%s-%03d", stamp, i),
                    String.format("SG-MULTI-%s-%03d", stamp, i),
                    String.format("multi-female-%03d", i),
                    2,
                    6,
                    6));
        }

        ObjectNode taskBody = objectMapper.createObjectNode();
        taskBody.put("sceneId", sceneId);
        taskBody.put("versionId", versionId);
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
        assertThat(waitTaskFinished(taskId, authorization)).isEqualTo("SUCCESS");

        JsonNode taskDetail = readData(mockMvc.perform(get("/cost/run/task/{taskId}", taskId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(taskDetail.path("task").path("sourceCount").asInt()).isEqualTo(501);
        assertThat(taskDetail.path("task").path("successCount").asInt()).isEqualTo(501);
        assertThat(taskDetail.path("task").path("failCount").asInt()).isEqualTo(0);
        assertThat(taskDetail.path("summary").path("partitionCount").asInt()).isEqualTo(2);
        assertThat(taskDetail.path("summary").path("failedPartitionCount").asInt()).isEqualTo(0);

        JsonNode partitionA = taskDetail.path("partitions").path(0);
        JsonNode partitionB = taskDetail.path("partitions").path(1);
        assertThat(partitionA.path("partitionNo").asInt()).isEqualTo(1);
        assertThat(partitionA.path("partitionStatus").asText()).isEqualTo("SUCCESS");
        assertThat(partitionA.path("startItemNo").asInt()).isEqualTo(1);
        assertThat(partitionA.path("endItemNo").asInt()).isEqualTo(500);
        assertThat(partitionA.path("totalCount").asInt()).isEqualTo(500);
        assertThat(partitionA.path("processedCount").asInt()).isEqualTo(500);
        assertThat(partitionA.path("successCount").asInt()).isEqualTo(500);
        assertThat(partitionA.path("failCount").asInt()).isEqualTo(0);

        assertThat(partitionB.path("partitionNo").asInt()).isEqualTo(2);
        assertThat(partitionB.path("partitionStatus").asText()).isEqualTo("SUCCESS");
        assertThat(partitionB.path("startItemNo").asInt()).isEqualTo(501);
        assertThat(partitionB.path("endItemNo").asInt()).isEqualTo(501);
        assertThat(partitionB.path("totalCount").asInt()).isEqualTo(1);
        assertThat(partitionB.path("processedCount").asInt()).isEqualTo(1);
        assertThat(partitionB.path("successCount").asInt()).isEqualTo(1);
        assertThat(partitionB.path("failCount").asInt()).isEqualTo(0);

        JsonNode resultList = readBody(mockMvc.perform(get("/cost/run/result/list")
                        .header("Authorization", authorization)
                        .param("requestNo", requestNo)
                        .param("feeCode", FEMALE_FEE_CODE)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(resultList.path("total").asInt()).isEqualTo(501);
        JsonNode firstLedger = findNodeByField(resultList.path("rows"), "objectCode", String.format("SG-MULTI-%s-%03d", stamp, 1));
        assertThat(firstLedger).isNotNull();
        assertThat(firstLedger.path("amountValue").decimalValue()).isEqualByComparingTo("7233.33");
    }

    @Test
    void shouldRetryFailedTaskDetailAndRefreshTaskSummary() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        CostFormula formula = requireFormula(sceneId, "SG_RULE_MANAGEMENT_FEE_AMOUNT");
        formula.setFormulaExpr("I.objectName.length()");
        formulaMapper.updateById(formula);
        Long versionId = publishScene(sceneId, authorization, "detail-retry-regression-" + stamp);

        try {
            ObjectNode validInput = createManagementInputItem("SG-DETAIL-BIZ-" + stamp + "-001",
                    "SG-DETAIL-" + stamp + "-A", "VALID-MGMT");
            ObjectNode failedInput = createManagementInputItem("SG-DETAIL-BIZ-" + stamp + "-002",
                    "SG-DETAIL-" + stamp + "-B", null);
            String requestNo = "SG-DETAIL-REQ-" + stamp;

            ObjectNode taskBody = objectMapper.createObjectNode();
            taskBody.put("sceneId", sceneId);
            taskBody.put("versionId", versionId);
            taskBody.put("taskType", "FORMAL_BATCH");
            taskBody.put("billMonth", YearMonth.now().plusMonths(2).toString());
            taskBody.put("requestNo", requestNo);
            taskBody.put("inputJson", objectMapper.writeValueAsString(objectMapper.createArrayNode().add(validInput).add(failedInput)));

            JsonNode taskSubmit = readData(mockMvc.perform(post("/cost/run/task/submit")
                            .header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(taskBody)))
                    .andExpect(status().isOk())
                    .andReturn());

            long taskId = taskSubmit.path("task").path("taskId").asLong();
            assertThat(waitTaskFinished(taskId, authorization)).isEqualTo("PART_SUCCESS");

            JsonNode taskDetail = readData(mockMvc.perform(get("/cost/run/task/{taskId}", taskId)
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(taskDetail.path("task").path("successCount").asInt()).isEqualTo(1);
            assertThat(taskDetail.path("task").path("failCount").asInt()).isEqualTo(1);
            assertThat(taskDetail.path("summary").path("retryableCount").asInt()).isEqualTo(1);

            JsonNode failedDetailNode = findNodeByField(taskDetail.path("details"), "bizNo", failedInput.path("bizNo").asText());
            assertThat(failedDetailNode).isNotNull();
            assertThat(failedDetailNode.path("detailStatus").asText()).isEqualTo("FAILED");

            CostCalcTaskDetail failedDetail = taskDetailMapper.selectById(failedDetailNode.path("detailId").asLong());
            failedDetail.setInputJson(objectMapper.writeValueAsString(createManagementInputItem(
                    failedInput.path("bizNo").asText(),
                    failedInput.path("objectCode").asText(),
                    "RETRY-DETAIL")));
            taskDetailMapper.updateById(failedDetail);

            JsonNode retryResponse = readBody(mockMvc.perform(put("/cost/run/task/retry/{detailId}", failedDetail.getDetailId())
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(retryResponse.path("code").asInt()).isEqualTo(200);
            assertThat(waitTaskStatus(taskId, authorization, "SUCCESS")).isEqualTo("SUCCESS");

            JsonNode retriedTask = readData(mockMvc.perform(get("/cost/run/task/{taskId}", taskId)
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(retriedTask.path("task").path("successCount").asInt()).isEqualTo(2);
            assertThat(retriedTask.path("task").path("failCount").asInt()).isEqualTo(0);
            assertThat(retriedTask.path("summary").path("retryableCount").asInt()).isEqualTo(0);

            JsonNode retriedDetailNode = findNodeByField(retriedTask.path("details"), "bizNo", failedInput.path("bizNo").asText());
            assertThat(retriedDetailNode).isNotNull();
            assertThat(retriedDetailNode.path("detailStatus").asText()).isEqualTo("SUCCESS");
            assertThat(retriedDetailNode.path("retryCount").asInt()).isEqualTo(1);

            JsonNode resultList = readBody(mockMvc.perform(get("/cost/run/result/list")
                            .header("Authorization", authorization)
                            .param("requestNo", requestNo)
                            .param("feeCode", MANAGEMENT_FEE_CODE)
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andReturn());

            JsonNode validLedger = findNodeByField(resultList.path("rows"), "objectCode", validInput.path("objectCode").asText());
            JsonNode retriedLedger = findNodeByField(resultList.path("rows"), "objectCode", failedInput.path("objectCode").asText());
            assertThat(validLedger).isNotNull();
            assertThat(retriedLedger).isNotNull();
            assertThat(validLedger.path("amountValue").decimalValue()).isEqualByComparingTo("10.00");
            assertThat(retriedLedger.path("amountValue").decimalValue()).isEqualByComparingTo("12.00");
        } finally {
            formula.setFormulaExpr(MANAGEMENT_FEE_FORMULA_BASELINE);
            formulaMapper.updateById(formula);
        }
    }

    @Test
    void shouldRetryFailedPartitionAndRefreshPartitionSummary() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        CostFormula formula = requireFormula(sceneId, "SG_RULE_MANAGEMENT_FEE_AMOUNT");
        formula.setFormulaExpr("I.objectName.length()");
        formulaMapper.updateById(formula);
        Long versionId = publishScene(sceneId, authorization, "partition-retry-regression-" + stamp);

        try {
            ObjectNode validInput = createManagementInputItem("SG-PART-BIZ-" + stamp + "-001",
                    "SG-PART-" + stamp + "-A", "VALID-PART");
            ObjectNode failedInput = createManagementInputItem("SG-PART-BIZ-" + stamp + "-002",
                    "SG-PART-" + stamp + "-B", null);
            String requestNo = "SG-PART-REQ-" + stamp;

            ObjectNode taskBody = objectMapper.createObjectNode();
            taskBody.put("sceneId", sceneId);
            taskBody.put("versionId", versionId);
            taskBody.put("taskType", "FORMAL_BATCH");
            taskBody.put("billMonth", YearMonth.now().plusMonths(2).toString());
            taskBody.put("requestNo", requestNo);
            taskBody.put("inputJson", objectMapper.writeValueAsString(objectMapper.createArrayNode().add(validInput).add(failedInput)));

            JsonNode taskSubmit = readData(mockMvc.perform(post("/cost/run/task/submit")
                            .header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(taskBody)))
                    .andExpect(status().isOk())
                    .andReturn());

            long taskId = taskSubmit.path("task").path("taskId").asLong();
            assertThat(waitTaskFinished(taskId, authorization)).isEqualTo("PART_SUCCESS");

            JsonNode taskDetail = readData(mockMvc.perform(get("/cost/run/task/{taskId}", taskId)
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            JsonNode failedPartitionNode = taskDetail.path("partitions").path(0);
            JsonNode failedDetailNode = findNodeByField(taskDetail.path("details"), "bizNo", failedInput.path("bizNo").asText());
            assertThat(failedPartitionNode.path("partitionStatus").asText()).isEqualTo("PART_SUCCESS");
            assertThat(failedPartitionNode.path("successCount").asInt()).isEqualTo(1);
            assertThat(failedPartitionNode.path("failCount").asInt()).isEqualTo(1);
            assertThat(failedDetailNode).isNotNull();

            CostCalcTaskDetail failedDetail = taskDetailMapper.selectById(failedDetailNode.path("detailId").asLong());
            failedDetail.setInputJson(objectMapper.writeValueAsString(createManagementInputItem(
                    failedInput.path("bizNo").asText(),
                    failedInput.path("objectCode").asText(),
                    "RETRY-PARTITION")));
            taskDetailMapper.updateById(failedDetail);

            CostCalcTaskPartition failedPartition = taskPartitionMapper.selectById(failedPartitionNode.path("partitionId").asLong());
            JsonNode retryResponse = readBody(mockMvc.perform(put("/cost/run/task/partition/retry/{partitionId}", failedPartition.getPartitionId())
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(retryResponse.path("code").asInt()).isEqualTo(200);
            assertThat(waitTaskStatus(taskId, authorization, "SUCCESS")).isEqualTo("SUCCESS");

            JsonNode retriedTask = readData(mockMvc.perform(get("/cost/run/task/{taskId}", taskId)
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            JsonNode retriedPartitionNode = retriedTask.path("partitions").path(0);
            JsonNode retriedDetailNode = findNodeByField(retriedTask.path("details"), "bizNo", failedInput.path("bizNo").asText());
            assertThat(retriedTask.path("task").path("successCount").asInt()).isEqualTo(2);
            assertThat(retriedTask.path("task").path("failCount").asInt()).isEqualTo(0);
            assertThat(retriedPartitionNode.path("partitionStatus").asText()).isEqualTo("SUCCESS");
            assertThat(retriedPartitionNode.path("processedCount").asInt()).isEqualTo(2);
            assertThat(retriedPartitionNode.path("successCount").asInt()).isEqualTo(2);
            assertThat(retriedPartitionNode.path("failCount").asInt()).isEqualTo(0);
            assertThat(retriedDetailNode).isNotNull();
            assertThat(retriedDetailNode.path("detailStatus").asText()).isEqualTo("SUCCESS");
            assertThat(retriedDetailNode.path("retryCount").asInt()).isEqualTo(1);

            JsonNode resultList = readBody(mockMvc.perform(get("/cost/run/result/list")
                            .header("Authorization", authorization)
                            .param("requestNo", requestNo)
                            .param("feeCode", MANAGEMENT_FEE_CODE)
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andReturn());

            JsonNode validLedger = findNodeByField(resultList.path("rows"), "objectCode", validInput.path("objectCode").asText());
            JsonNode retriedLedger = findNodeByField(resultList.path("rows"), "objectCode", failedInput.path("objectCode").asText());
            assertThat(validLedger).isNotNull();
            assertThat(retriedLedger).isNotNull();
            assertThat(validLedger.path("amountValue").decimalValue()).isEqualByComparingTo("10.00");
            assertThat(retriedLedger.path("amountValue").decimalValue()).isEqualByComparingTo("15.00");
        } finally {
            formula.setFormulaExpr(MANAGEMENT_FEE_FORMULA_BASELINE);
            formulaMapper.updateById(formula);
        }
    }

    @Test
    void shouldCancelRunningFormalTask() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "cancel-task-regression-" + stamp);
        String requestNo = "SG-CANCEL-REQ-" + stamp;
        String billMonth = YearMonth.now().plusMonths(2).toString();

        ArrayNode inputItems = objectMapper.createArrayNode();
        for (int i = 1; i <= 1501; i++) {
            inputItems.add(createFemaleInputItem(
                    String.format("SG-CANCEL-BIZ-%s-%04d", stamp, i),
                    String.format("SG-CANCEL-%s-%04d", stamp, i),
                    String.format("cancel-female-%04d", i),
                    2,
                    6,
                    6));
        }

        ObjectNode taskBody = objectMapper.createObjectNode();
        taskBody.put("sceneId", sceneId);
        taskBody.put("versionId", versionId);
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
        assertThat(waitTaskStatus(taskId, authorization, "RUNNING")).isEqualTo("RUNNING");

        JsonNode cancelResponse = readBody(mockMvc.perform(put("/cost/run/task/cancel/{taskId}", taskId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(cancelResponse.path("code").asInt()).isEqualTo(200);
        assertThat(waitTaskStatus(taskId, authorization, "CANCELLED")).isEqualTo("CANCELLED");

        JsonNode taskDetail = readData(mockMvc.perform(get("/cost/run/task/{taskId}", taskId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(taskDetail.path("task").path("taskStatus").asText()).isEqualTo("CANCELLED");
        assertThat(taskDetail.path("task").path("sourceCount").asInt()).isEqualTo(1501);
        assertThat(taskDetail.path("summary").path("partitionCount").asInt()).isEqualTo(4);
        assertThat(taskDetail.path("partitions").isArray()).isTrue();
        assertThat(taskDetail.path("partitions").size()).isEqualTo(4);
        Set<String> partitionStatuses = readFieldSet(taskDetail.path("partitions"), "partitionStatus");
        assertThat(partitionStatuses).contains("CANCELLED");
        assertThat(partitionStatuses).doesNotContain("INIT", "RUNNING");

        JsonNode resultList = readBody(mockMvc.perform(get("/cost/run/result/list")
                        .header("Authorization", authorization)
                        .param("requestNo", requestNo)
                        .param("feeCode", FEMALE_FEE_CODE)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(resultList.path("total").asInt()).isLessThan(1501);
    }

    @Test
    void shouldRecoverStaleRunningTaskByDispatchCoordinator() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "stale-task-recovery-" + stamp);
        String requestNo = "SG-STALE-REQ-" + stamp;
        String billMonth = YearMonth.now().plusMonths(2).toString();

        ArrayNode inputItems = objectMapper.createArrayNode();
        inputItems.add(createFemaleInputItem("SG-STALE-BIZ-" + stamp + "-001", "SG-STALE-" + stamp + "-A", "stale-a", 2, 6, 6));
        inputItems.add(createFemaleInputItem("SG-STALE-BIZ-" + stamp + "-002", "SG-STALE-" + stamp + "-B", "stale-b", 1, 5, 5));

        ObjectNode taskBody = objectMapper.createObjectNode();
        taskBody.put("sceneId", sceneId);
        taskBody.put("versionId", versionId);
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
        assertThat(waitTaskFinished(taskId, authorization)).isEqualTo("SUCCESS");

        Date staleTime = new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(11));
        taskMapper.update(null, Wrappers.<CostCalcTask>lambdaUpdate()
                .eq(CostCalcTask::getTaskId, taskId)
                .set(CostCalcTask::getTaskStatus, "RUNNING")
                .set(CostCalcTask::getSuccessCount, 0)
                .set(CostCalcTask::getFailCount, 0)
                .set(CostCalcTask::getProgressPercent, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .set(CostCalcTask::getStartedTime, staleTime)
                .set(CostCalcTask::getFinishedTime, null)
                .set(CostCalcTask::getDurationMs, 0L)
                .set(CostCalcTask::getExecuteNode, "STALE-NODE")
                .set(CostCalcTask::getErrorMessage, "")
                .set(CostCalcTask::getUpdateTime, staleTime));
        taskPartitionMapper.update(null, Wrappers.<CostCalcTaskPartition>lambdaUpdate()
                .eq(CostCalcTaskPartition::getTaskId, taskId)
                .set(CostCalcTaskPartition::getPartitionStatus, "RUNNING")
                .set(CostCalcTaskPartition::getProcessedCount, 0)
                .set(CostCalcTaskPartition::getSuccessCount, 0)
                .set(CostCalcTaskPartition::getFailCount, 0)
                .set(CostCalcTaskPartition::getStartedTime, staleTime)
                .set(CostCalcTaskPartition::getFinishedTime, null)
                .set(CostCalcTaskPartition::getDurationMs, 0L)
                .set(CostCalcTaskPartition::getUpdateTime, staleTime));

        ReflectionTestUtils.invokeMethod(costRunService, "dispatchRecoverableTasksOnce");

        assertThat(waitTaskFinished(taskId, authorization)).isEqualTo("SUCCESS");

        CostCalcTask recoveredTask = taskMapper.selectById(taskId);
        assertThat(recoveredTask).isNotNull();
        assertThat(recoveredTask.getTaskStatus()).isEqualTo("SUCCESS");
        assertThat(recoveredTask.getExecuteNode()).isEqualTo(resolveExpectedExecuteNode());
        assertThat(recoveredTask.getSuccessCount()).isEqualTo(2);

        Set<String> partitionStatuses = taskPartitionMapper.selectList(Wrappers.<CostCalcTaskPartition>lambdaQuery()
                        .eq(CostCalcTaskPartition::getTaskId, taskId))
                .stream()
                .map(CostCalcTaskPartition::getPartitionStatus)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        assertThat(partitionStatuses).containsExactly("SUCCESS");
    }

    @Test
    void shouldSkipDuplicateTaskDispatchWhenDispatchLockIsOccupied() throws Exception {
        Long taskId = -1L;
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicBoolean secondExecuted = new AtomicBoolean(false);
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try {
            Future<Boolean> firstDispatch = executorService.submit(() ->
                    distributedLockSupport.executeTaskDispatchLockOrSkip(taskId, () ->
                    {
                        firstEntered.countDown();
                        try {
                            releaseFirst.await(5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                    }));

            assertThat(firstEntered.await(5, TimeUnit.SECONDS)).isTrue();

            boolean secondDispatch = distributedLockSupport.executeTaskDispatchLockOrSkip(taskId,
                    () -> secondExecuted.set(true));

            assertThat(secondDispatch).isFalse();
            assertThat(secondExecuted).isFalse();

            releaseFirst.countDown();
            assertThat(firstDispatch.get(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            releaseFirst.countDown();
            executorService.shutdownNow();
        }
    }

    @Test
    void shouldSkipDuplicateRecoveryDispatchCoordinatorWhenLockIsOccupied() throws Exception {
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicBoolean secondExecuted = new AtomicBoolean(false);
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try {
            Future<Boolean> firstDispatch = executorService.submit(() ->
                    distributedLockSupport.executeTaskDispatchCoordinatorLockOrSkip(() ->
                    {
                        firstEntered.countDown();
                        try {
                            releaseFirst.await(5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                    }));

            assertThat(firstEntered.await(5, TimeUnit.SECONDS)).isTrue();

            boolean secondDispatch = distributedLockSupport.executeTaskDispatchCoordinatorLockOrSkip(
                    () -> secondExecuted.set(true));

            assertThat(secondDispatch).isFalse();
            assertThat(secondExecuted).isFalse();

            releaseFirst.countDown();
            assertThat(firstDispatch.get(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            releaseFirst.countDown();
            executorService.shutdownNow();
        }
    }

    @Test
    void shouldClaimPartitionExecutionOnlyOnceAndPersistOwnerMetadata() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "partition-claim-metadata-" + stamp);
        String billMonth = YearMonth.now().plusMonths(2).toString();

        ArrayNode inputItems = objectMapper.createArrayNode();
        inputItems.add(createFemaleInputItem(
                "SG-PARTITION-CLAIM-BIZ-" + stamp + "-001",
                "SG-PARTITION-CLAIM-" + stamp + "-A",
                "partition-claim-a",
                1,
                6,
                6));

        ObjectNode taskBody = objectMapper.createObjectNode();
        taskBody.put("sceneId", sceneId);
        taskBody.put("versionId", versionId);
        taskBody.put("taskType", "FORMAL_BATCH");
        taskBody.put("billMonth", billMonth);
        taskBody.put("requestNo", "SG-PARTITION-CLAIM-REQ-" + stamp);
        taskBody.put("inputJson", objectMapper.writeValueAsString(inputItems));

        JsonNode taskSubmit = readData(mockMvc.perform(post("/cost/run/task/submit")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(taskBody)))
                .andExpect(status().isOk())
                .andReturn());
        long taskId = taskSubmit.path("task").path("taskId").asLong();
        assertThat(waitTaskFinished(taskId, authorization)).isEqualTo("SUCCESS");

        CostCalcTaskPartition originalPartition = taskPartitionMapper.selectOne(Wrappers.<CostCalcTaskPartition>lambdaQuery()
                .eq(CostCalcTaskPartition::getTaskId, taskId)
                .last("limit 1"));
        assertThat(originalPartition).isNotNull();

        Date resetTime = new Date(System.currentTimeMillis() - 1_000L);
        taskPartitionMapper.update(null, Wrappers.<CostCalcTaskPartition>lambdaUpdate()
                .eq(CostCalcTaskPartition::getPartitionId, originalPartition.getPartitionId())
                .set(CostCalcTaskPartition::getPartitionStatus, "INIT")
                .set(CostCalcTaskPartition::getExecuteNode, null)
                .set(CostCalcTaskPartition::getClaimTime, null)
                .set(CostCalcTaskPartition::getStartedTime, null)
                .set(CostCalcTaskPartition::getFinishedTime, null)
                .set(CostCalcTaskPartition::getDurationMs, 0L)
                .set(CostCalcTaskPartition::getRecoveryHint, "")
                .set(CostCalcTaskPartition::getLastErrorStage, "")
                .set(CostCalcTaskPartition::getLastError, "")
                .set(CostCalcTaskPartition::getUpdateTime, resetTime));

        Boolean firstClaim = ReflectionTestUtils.invokeMethod(costRunService,
                "tryClaimPartitionExecution", taskId, originalPartition.getPartitionNo(), resetTime);
        Boolean secondClaim = ReflectionTestUtils.invokeMethod(costRunService,
                "tryClaimPartitionExecution", taskId, originalPartition.getPartitionNo(), new Date());

        assertThat(firstClaim).isTrue();
        assertThat(secondClaim).isFalse();

        CostCalcTaskPartition claimedPartition = taskPartitionMapper.selectById(originalPartition.getPartitionId());
        assertThat(claimedPartition).isNotNull();
        assertThat(claimedPartition.getPartitionStatus()).isEqualTo("RUNNING");
        assertThat(claimedPartition.getExecuteNode()).isEqualTo(resolveExpectedExecuteNode());
        assertThat(claimedPartition.getClaimTime()).isNotNull();
        assertThat(claimedPartition.getStartedTime()).isEqualTo(claimedPartition.getClaimTime());
    }

    @Test
    void shouldInvalidateOldPartitionClaimAfterPartitionIsReset() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "partition-claim-reset-" + stamp);
        String billMonth = YearMonth.now().plusMonths(2).toString();

        ArrayNode inputItems = objectMapper.createArrayNode();
        inputItems.add(createFemaleInputItem(
                "SG-PARTITION-RESET-BIZ-" + stamp + "-001",
                "SG-PARTITION-RESET-" + stamp + "-A",
                "partition-reset-a",
                1,
                6,
                6));

        ObjectNode taskBody = objectMapper.createObjectNode();
        taskBody.put("sceneId", sceneId);
        taskBody.put("versionId", versionId);
        taskBody.put("taskType", "FORMAL_BATCH");
        taskBody.put("billMonth", billMonth);
        taskBody.put("requestNo", "SG-PARTITION-RESET-REQ-" + stamp);
        taskBody.put("inputJson", objectMapper.writeValueAsString(inputItems));

        JsonNode taskSubmit = readData(mockMvc.perform(post("/cost/run/task/submit")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(taskBody)))
                .andExpect(status().isOk())
                .andReturn());
        long taskId = taskSubmit.path("task").path("taskId").asLong();
        assertThat(waitTaskFinished(taskId, authorization)).isEqualTo("SUCCESS");

        CostCalcTaskPartition partition = taskPartitionMapper.selectOne(Wrappers.<CostCalcTaskPartition>lambdaQuery()
                .eq(CostCalcTaskPartition::getTaskId, taskId)
                .last("limit 1"));
        assertThat(partition).isNotNull();
        List<CostCalcTaskDetail> partitionDetails = taskDetailMapper.selectList(Wrappers.<CostCalcTaskDetail>lambdaQuery()
                .eq(CostCalcTaskDetail::getTaskId, taskId)
                .eq(CostCalcTaskDetail::getPartitionNo, partition.getPartitionNo())
                .orderByAsc(CostCalcTaskDetail::getDetailId));
        assertThat(partitionDetails).isNotEmpty();

        Date resetTime = new Date(System.currentTimeMillis() - 1_000L);
        taskPartitionMapper.update(null, Wrappers.<CostCalcTaskPartition>lambdaUpdate()
                .eq(CostCalcTaskPartition::getPartitionId, partition.getPartitionId())
                .set(CostCalcTaskPartition::getPartitionStatus, "INIT")
                .set(CostCalcTaskPartition::getExecuteNode, null)
                .set(CostCalcTaskPartition::getClaimTime, null)
                .set(CostCalcTaskPartition::getStartedTime, null)
                .set(CostCalcTaskPartition::getFinishedTime, null)
                .set(CostCalcTaskPartition::getDurationMs, 0L)
                .set(CostCalcTaskPartition::getRecoveryHint, "")
                .set(CostCalcTaskPartition::getLastErrorStage, "")
                .set(CostCalcTaskPartition::getLastError, "")
                .set(CostCalcTaskPartition::getUpdateTime, resetTime));

        Object claimToken = ReflectionTestUtils.invokeMethod(costRunService,
                "markPartitionRunning", taskId, partitionDetails);
        Boolean ownedBeforeReset = ReflectionTestUtils.invokeMethod(costRunService,
                "isPartitionClaimOwned", claimToken);

        taskPartitionMapper.update(null, Wrappers.<CostCalcTaskPartition>lambdaUpdate()
                .eq(CostCalcTaskPartition::getPartitionId, partition.getPartitionId())
                .set(CostCalcTaskPartition::getPartitionStatus, "INIT")
                .set(CostCalcTaskPartition::getExecuteNode, null)
                .set(CostCalcTaskPartition::getClaimTime, null)
                .set(CostCalcTaskPartition::getStartedTime, null)
                .set(CostCalcTaskPartition::getFinishedTime, null)
                .set(CostCalcTaskPartition::getDurationMs, 0L)
                .set(CostCalcTaskPartition::getUpdateTime, DateUtils.getNowDate()));

        Boolean ownedAfterReset = ReflectionTestUtils.invokeMethod(costRunService,
                "isPartitionClaimOwned", claimToken);

        assertThat(ownedBeforeReset).isTrue();
        assertThat(ownedAfterReset).isFalse();
    }

    @Test
    void shouldSkipFinishPartitionWhenOldClaimTokenLosesOwnership() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "partition-finish-guard-" + stamp);
        String billMonth = YearMonth.now().plusMonths(2).toString();

        ArrayNode inputItems = objectMapper.createArrayNode();
        inputItems.add(createFemaleInputItem(
                "SG-PARTITION-FINISH-BIZ-" + stamp + "-001",
                "SG-PARTITION-FINISH-" + stamp + "-A",
                "partition-finish-a",
                1,
                6,
                6));

        ObjectNode taskBody = objectMapper.createObjectNode();
        taskBody.put("sceneId", sceneId);
        taskBody.put("versionId", versionId);
        taskBody.put("taskType", "FORMAL_BATCH");
        taskBody.put("billMonth", billMonth);
        taskBody.put("requestNo", "SG-PARTITION-FINISH-REQ-" + stamp);
        taskBody.put("inputJson", objectMapper.writeValueAsString(inputItems));

        JsonNode taskSubmit = readData(mockMvc.perform(post("/cost/run/task/submit")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(taskBody)))
                .andExpect(status().isOk())
                .andReturn());
        long taskId = taskSubmit.path("task").path("taskId").asLong();
        assertThat(waitTaskFinished(taskId, authorization)).isEqualTo("SUCCESS");

        CostCalcTaskPartition partition = taskPartitionMapper.selectOne(Wrappers.<CostCalcTaskPartition>lambdaQuery()
                .eq(CostCalcTaskPartition::getTaskId, taskId)
                .last("limit 1"));
        assertThat(partition).isNotNull();
        List<CostCalcTaskDetail> partitionDetails = taskDetailMapper.selectList(Wrappers.<CostCalcTaskDetail>lambdaQuery()
                .eq(CostCalcTaskDetail::getTaskId, taskId)
                .eq(CostCalcTaskDetail::getPartitionNo, partition.getPartitionNo())
                .orderByAsc(CostCalcTaskDetail::getDetailId));
        assertThat(partitionDetails).isNotEmpty();

        Date claimTime = new Date(System.currentTimeMillis() - 2_000L);
        taskPartitionMapper.update(null, Wrappers.<CostCalcTaskPartition>lambdaUpdate()
                .eq(CostCalcTaskPartition::getPartitionId, partition.getPartitionId())
                .set(CostCalcTaskPartition::getPartitionStatus, "INIT")
                .set(CostCalcTaskPartition::getExecuteNode, null)
                .set(CostCalcTaskPartition::getClaimTime, null)
                .set(CostCalcTaskPartition::getStartedTime, null)
                .set(CostCalcTaskPartition::getFinishedTime, null)
                .set(CostCalcTaskPartition::getDurationMs, 0L)
                .set(CostCalcTaskPartition::getProcessedCount, 0)
                .set(CostCalcTaskPartition::getSuccessCount, 0)
                .set(CostCalcTaskPartition::getFailCount, 0)
                .set(CostCalcTaskPartition::getAmountTotal, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .set(CostCalcTaskPartition::getUpdateTime, claimTime));

        Object oldClaimToken = ReflectionTestUtils.invokeMethod(costRunService,
                "markPartitionRunning", taskId, partitionDetails);

        taskPartitionMapper.update(null, Wrappers.<CostCalcTaskPartition>lambdaUpdate()
                .eq(CostCalcTaskPartition::getPartitionId, partition.getPartitionId())
                .set(CostCalcTaskPartition::getPartitionStatus, "INIT")
                .set(CostCalcTaskPartition::getExecuteNode, null)
                .set(CostCalcTaskPartition::getClaimTime, null)
                .set(CostCalcTaskPartition::getStartedTime, null)
                .set(CostCalcTaskPartition::getFinishedTime, null)
                .set(CostCalcTaskPartition::getDurationMs, 0L)
                .set(CostCalcTaskPartition::getProcessedCount, 0)
                .set(CostCalcTaskPartition::getSuccessCount, 0)
                .set(CostCalcTaskPartition::getFailCount, 0)
                .set(CostCalcTaskPartition::getAmountTotal, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .set(CostCalcTaskPartition::getUpdateTime, DateUtils.getNowDate()));

        Class<?> resultType = Class.forName("com.ruoyi.system.service.impl.cost.CostRunServiceImpl$PartitionExecutionResult");
        var resultConstructor = resultType.getDeclaredConstructor();
        resultConstructor.setAccessible(true);
        Object fakeResult = resultConstructor.newInstance();
        ReflectionTestUtils.setField(fakeResult, "processedCount", 1);
        ReflectionTestUtils.setField(fakeResult, "successCount", 1);
        ReflectionTestUtils.setField(fakeResult, "failedCount", 0);
        ReflectionTestUtils.setField(fakeResult, "amountTotal", BigDecimal.TEN.setScale(2, RoundingMode.HALF_UP));
        ReflectionTestUtils.setField(fakeResult, "persistMode", "BATCH");
        ReflectionTestUtils.setField(fakeResult, "recoveryHint", "");
        ReflectionTestUtils.setField(fakeResult, "lastErrorStage", "");

        ReflectionTestUtils.invokeMethod(costRunService, "finishPartition",
                taskId, partitionDetails, oldClaimToken, fakeResult, null);

        CostCalcTaskPartition guardedPartition = taskPartitionMapper.selectById(partition.getPartitionId());
        assertThat(guardedPartition).isNotNull();
        assertThat(guardedPartition.getPartitionStatus()).isEqualTo("INIT");
        assertThat(guardedPartition.getProcessedCount()).isZero();
        assertThat(guardedPartition.getSuccessCount()).isZero();
        assertThat(guardedPartition.getFailCount()).isZero();
        assertThat(guardedPartition.getAmountTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldAllowNewConfiguredNodeToTakeOverPartitionAfterReset() throws Exception {
        String originalNodeId = System.getProperty("cost.dispatch.node-id");
        try {
            Long sceneId = requireSceneId();
            String token = loginAndGetToken();
            String authorization = "Bearer " + token;
            String stamp = LocalTime.now().format(STAMP_FORMATTER);
            Long versionId = publishScene(sceneId, authorization, "partition-node-takeover-" + stamp);
            String billMonth = YearMonth.now().plusMonths(2).toString();

            ArrayNode inputItems = objectMapper.createArrayNode();
            inputItems.add(createFemaleInputItem(
                    "SG-PARTITION-NODE-BIZ-" + stamp + "-001",
                    "SG-PARTITION-NODE-" + stamp + "-A",
                    "partition-node-a",
                    1,
                    6,
                    6));

            ObjectNode taskBody = objectMapper.createObjectNode();
            taskBody.put("sceneId", sceneId);
            taskBody.put("versionId", versionId);
            taskBody.put("taskType", "FORMAL_BATCH");
            taskBody.put("billMonth", billMonth);
            taskBody.put("requestNo", "SG-PARTITION-NODE-REQ-" + stamp);
            taskBody.put("inputJson", objectMapper.writeValueAsString(inputItems));

            JsonNode taskSubmit = readData(mockMvc.perform(post("/cost/run/task/submit")
                            .header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(taskBody)))
                    .andExpect(status().isOk())
                    .andReturn());
            long taskId = taskSubmit.path("task").path("taskId").asLong();
            assertThat(waitTaskFinished(taskId, authorization)).isEqualTo("SUCCESS");

            CostCalcTaskPartition partition = taskPartitionMapper.selectOne(Wrappers.<CostCalcTaskPartition>lambdaQuery()
                    .eq(CostCalcTaskPartition::getTaskId, taskId)
                    .last("limit 1"));
            assertThat(partition).isNotNull();
            List<CostCalcTaskDetail> partitionDetails = taskDetailMapper.selectList(Wrappers.<CostCalcTaskDetail>lambdaQuery()
                    .eq(CostCalcTaskDetail::getTaskId, taskId)
                    .eq(CostCalcTaskDetail::getPartitionNo, partition.getPartitionNo())
                    .orderByAsc(CostCalcTaskDetail::getDetailId));
            assertThat(partitionDetails).isNotEmpty();

            Date resetTime = new Date(System.currentTimeMillis() - 1_000L);
            taskPartitionMapper.update(null, Wrappers.<CostCalcTaskPartition>lambdaUpdate()
                    .eq(CostCalcTaskPartition::getPartitionId, partition.getPartitionId())
                    .set(CostCalcTaskPartition::getPartitionStatus, "INIT")
                    .set(CostCalcTaskPartition::getExecuteNode, null)
                    .set(CostCalcTaskPartition::getClaimTime, null)
                    .set(CostCalcTaskPartition::getStartedTime, null)
                    .set(CostCalcTaskPartition::getFinishedTime, null)
                    .set(CostCalcTaskPartition::getDurationMs, 0L)
                    .set(CostCalcTaskPartition::getUpdateTime, resetTime));

            System.setProperty("cost.dispatch.node-id", "NODE-A");
            Object oldClaimToken = ReflectionTestUtils.invokeMethod(costRunService,
                    "markPartitionRunning", taskId, partitionDetails);

            taskPartitionMapper.update(null, Wrappers.<CostCalcTaskPartition>lambdaUpdate()
                    .eq(CostCalcTaskPartition::getPartitionId, partition.getPartitionId())
                    .set(CostCalcTaskPartition::getPartitionStatus, "INIT")
                    .set(CostCalcTaskPartition::getExecuteNode, null)
                    .set(CostCalcTaskPartition::getClaimTime, null)
                    .set(CostCalcTaskPartition::getStartedTime, null)
                    .set(CostCalcTaskPartition::getFinishedTime, null)
                    .set(CostCalcTaskPartition::getDurationMs, 0L)
                    .set(CostCalcTaskPartition::getUpdateTime, DateUtils.getNowDate()));

            System.setProperty("cost.dispatch.node-id", "NODE-B");
            Object newClaimToken = ReflectionTestUtils.invokeMethod(costRunService,
                    "markPartitionRunning", taskId, partitionDetails);

            Boolean oldOwned = ReflectionTestUtils.invokeMethod(costRunService,
                    "isPartitionClaimOwned", oldClaimToken);
            Boolean newOwned = ReflectionTestUtils.invokeMethod(costRunService,
                    "isPartitionClaimOwned", newClaimToken);

            CostCalcTaskPartition takenOverPartition = taskPartitionMapper.selectById(partition.getPartitionId());
            assertThat(oldOwned).isFalse();
            assertThat(newOwned).isTrue();
            assertThat(takenOverPartition).isNotNull();
            assertThat(takenOverPartition.getExecuteNode()).isEqualTo("NODE-B");
        } finally {
            if (originalNodeId == null) {
                System.clearProperty("cost.dispatch.node-id");
            } else {
                System.setProperty("cost.dispatch.node-id", originalNodeId);
            }
        }
    }

    @Test
    void shouldResolveExecuteNodeFromSpringPropertyBeforeSystemProperty() {
        Object originalEnvironment = ReflectionTestUtils.getField(costRunService, "environment");
        String originalConfiguredNodeId = costDispatchProperties.getNodeId();
        Long originalDispatchIntervalSeconds = costDispatchProperties.getDispatchIntervalSeconds();
        Long originalStaleTimeoutSeconds = costDispatchProperties.getStaleTimeoutSeconds();
        String originalNodeId = System.getProperty("cost.dispatch.node-id");
        try {
            MockEnvironment mockEnvironment = new MockEnvironment();
            mockEnvironment.setProperty("cost.dispatch.node-id", "NODE-CONFIG");
            mockEnvironment.setProperty("spring.application.name", "cost-platform-admin");
            mockEnvironment.setProperty("server.port", "8088");
            ReflectionTestUtils.setField(costRunService, "environment", mockEnvironment);
            costDispatchProperties.setNodeId("NODE-PROPERTIES");
            costDispatchProperties.setDispatchIntervalSeconds(7L);
            costDispatchProperties.setStaleTimeoutSeconds(45L);
            System.setProperty("cost.dispatch.node-id", "NODE-SYSTEM");

            String executeNode = ReflectionTestUtils.invokeMethod(costRunService, "resolveExecuteNode");
            Long dispatchIntervalSeconds = ReflectionTestUtils.invokeMethod(costRunService,
                    "resolveTaskDispatchIntervalSeconds");
            Long staleTimeoutSeconds = ReflectionTestUtils.invokeMethod(costRunService,
                    "resolveTaskStaleTimeoutSeconds");
            assertThat(executeNode).isEqualTo("NODE-PROPERTIES");
            assertThat(dispatchIntervalSeconds).isEqualTo(7L);
            assertThat(staleTimeoutSeconds).isEqualTo(45L);
        } finally {
            ReflectionTestUtils.setField(costRunService, "environment", originalEnvironment);
            costDispatchProperties.setNodeId(originalConfiguredNodeId);
            costDispatchProperties.setDispatchIntervalSeconds(originalDispatchIntervalSeconds);
            costDispatchProperties.setStaleTimeoutSeconds(originalStaleTimeoutSeconds);
            if (originalNodeId == null) {
                System.clearProperty("cost.dispatch.node-id");
            } else {
                System.setProperty("cost.dispatch.node-id", originalNodeId);
            }
        }
    }

    @Test
    void shouldExposePartitionOwnerOverviewWithStaleAndOwnerlessRunningPartitions() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "partition-owner-overview-" + stamp);
        String billMonth = YearMonth.now().plusMonths(2).toString();

        ArrayNode inputItems = objectMapper.createArrayNode();
        for (int index = 1; index <= 501; index++) {
            inputItems.add(createFemaleInputItem(
                    "SG-OWNER-OVERVIEW-BIZ-" + stamp + "-" + String.format("%03d", index),
                    "SG-OWNER-OVERVIEW-" + stamp + "-" + index,
                    "owner-overview-" + index,
                    1,
                    6,
                    6));
        }

        ObjectNode taskBody = objectMapper.createObjectNode();
        taskBody.put("sceneId", sceneId);
        taskBody.put("versionId", versionId);
        taskBody.put("taskType", "FORMAL_BATCH");
        taskBody.put("billMonth", billMonth);
        taskBody.put("requestNo", "SG-OWNER-OVERVIEW-REQ-" + stamp);
        taskBody.put("inputJson", objectMapper.writeValueAsString(inputItems));

        JsonNode taskSubmit = readData(mockMvc.perform(post("/cost/run/task/submit")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(taskBody)))
                .andExpect(status().isOk())
                .andReturn());
        long taskId = taskSubmit.path("task").path("taskId").asLong();
        assertThat(waitTaskFinished(taskId, authorization)).isEqualTo("SUCCESS");

        List<CostCalcTaskPartition> partitions = taskPartitionMapper.selectList(Wrappers.<CostCalcTaskPartition>lambdaQuery()
                .eq(CostCalcTaskPartition::getTaskId, taskId)
                .orderByAsc(CostCalcTaskPartition::getPartitionNo));
        assertThat(partitions).hasSizeGreaterThanOrEqualTo(2);

        Date staleClaimTime = new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(20));
        taskPartitionMapper.update(null, Wrappers.<CostCalcTaskPartition>lambdaUpdate()
                .eq(CostCalcTaskPartition::getPartitionId, partitions.get(0).getPartitionId())
                .set(CostCalcTaskPartition::getPartitionStatus, "RUNNING")
                .set(CostCalcTaskPartition::getExecuteNode, "NODE-STALE")
                .set(CostCalcTaskPartition::getClaimTime, staleClaimTime)
                .set(CostCalcTaskPartition::getStartedTime, staleClaimTime)
                .set(CostCalcTaskPartition::getFinishedTime, null)
                .set(CostCalcTaskPartition::getUpdateTime, staleClaimTime));
        taskPartitionMapper.update(null, Wrappers.<CostCalcTaskPartition>lambdaUpdate()
                .eq(CostCalcTaskPartition::getPartitionId, partitions.get(1).getPartitionId())
                .set(CostCalcTaskPartition::getPartitionStatus, "RUNNING")
                .set(CostCalcTaskPartition::getExecuteNode, null)
                .set(CostCalcTaskPartition::getClaimTime, null)
                .set(CostCalcTaskPartition::getStartedTime, staleClaimTime)
                .set(CostCalcTaskPartition::getFinishedTime, null)
                .set(CostCalcTaskPartition::getUpdateTime, staleClaimTime));

        JsonNode overview = readData(mockMvc.perform(get("/cost/run/task/overview")
                        .header("Authorization", authorization)
                        .param("taskId", String.valueOf(taskId))
                        .param("sceneId", String.valueOf(sceneId))
                        .param("billMonth", billMonth))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode ownerSummary = overview.path("ownerSummary");
        assertThat(ownerSummary.path("claimedPartitionCount").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(ownerSummary.path("activeOwnerCount").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(ownerSummary.path("staleRunningOwnerCount").asInt()).isEqualTo(1);
        assertThat(ownerSummary.path("runningWithoutOwnerCount").asInt()).isEqualTo(1);

        JsonNode ownerNode = findNodeByField(overview.path("partitionOwnerDistribution"), "executeNode", "NODE-STALE");
        assertThat(ownerNode).isNotNull();
        assertThat(ownerNode.path("partitionCount").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(ownerNode.path("runningCount").asInt()).isGreaterThanOrEqualTo(1);

        JsonNode riskTask = findNodeByField(overview.path("topOwnerRiskTasks"), "taskId", String.valueOf(taskId));
        assertThat(riskTask).isNotNull();
        assertThat(riskTask.path("staleRunningOwnerCount").asInt()).isEqualTo(1);
        assertThat(riskTask.path("runningWithoutOwnerCount").asInt()).isEqualTo(1);

        JsonNode taskDetail = readData(mockMvc.perform(get("/cost/run/task/{taskId}", taskId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode detailSummary = taskDetail.path("summary");
        assertThat(detailSummary.path("claimedPartitionCount").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(detailSummary.path("staleRunningOwnerCount").asInt()).isEqualTo(1);
        assertThat(detailSummary.path("runningWithoutOwnerCount").asInt()).isEqualTo(1);
    }

    @Test
    void shouldExposeMultiNodePartitionOwnershipForSameTask() throws Exception {
        String originalNodeId = System.getProperty("cost.dispatch.node-id");
        try {
            Long sceneId = requireSceneId();
            String token = loginAndGetToken();
            String authorization = "Bearer " + token;
            String stamp = LocalTime.now().format(STAMP_FORMATTER);
            Long versionId = publishScene(sceneId, authorization, "partition-multi-node-overview-" + stamp);
            String billMonth = YearMonth.now().plusMonths(2).toString();

            ArrayNode inputItems = objectMapper.createArrayNode();
            for (int index = 1; index <= 1001; index++) {
                inputItems.add(createFemaleInputItem(
                        "SG-MULTI-NODE-BIZ-" + stamp + "-" + String.format("%04d", index),
                        "SG-MULTI-NODE-" + stamp + "-" + index,
                        "multi-node-" + index,
                        1,
                        6,
                        6));
            }

            ObjectNode taskBody = objectMapper.createObjectNode();
            taskBody.put("sceneId", sceneId);
            taskBody.put("versionId", versionId);
            taskBody.put("taskType", "FORMAL_BATCH");
            taskBody.put("billMonth", billMonth);
            taskBody.put("requestNo", "SG-MULTI-NODE-REQ-" + stamp);
            taskBody.put("inputJson", objectMapper.writeValueAsString(inputItems));

            JsonNode taskSubmit = readData(mockMvc.perform(post("/cost/run/task/submit")
                            .header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(taskBody)))
                    .andExpect(status().isOk())
                    .andReturn());
            long taskId = taskSubmit.path("task").path("taskId").asLong();
            assertThat(waitTaskFinished(taskId, authorization)).isEqualTo("SUCCESS");

            List<CostCalcTaskPartition> partitions = taskPartitionMapper.selectList(Wrappers.<CostCalcTaskPartition>lambdaQuery()
                    .eq(CostCalcTaskPartition::getTaskId, taskId)
                    .orderByAsc(CostCalcTaskPartition::getPartitionNo));
            assertThat(partitions).hasSizeGreaterThanOrEqualTo(3);

            for (int index = 0; index < 2; index++) {
                taskPartitionMapper.update(null, Wrappers.<CostCalcTaskPartition>lambdaUpdate()
                        .eq(CostCalcTaskPartition::getPartitionId, partitions.get(index).getPartitionId())
                        .set(CostCalcTaskPartition::getPartitionStatus, "INIT")
                        .set(CostCalcTaskPartition::getExecuteNode, null)
                        .set(CostCalcTaskPartition::getClaimTime, null)
                        .set(CostCalcTaskPartition::getStartedTime, null)
                        .set(CostCalcTaskPartition::getFinishedTime, null)
                        .set(CostCalcTaskPartition::getDurationMs, 0L)
                        .set(CostCalcTaskPartition::getUpdateTime, DateUtils.getNowDate()));
            }

            List<CostCalcTaskDetail> firstPartitionDetails = taskDetailMapper.selectList(Wrappers.<CostCalcTaskDetail>lambdaQuery()
                    .eq(CostCalcTaskDetail::getTaskId, taskId)
                    .eq(CostCalcTaskDetail::getPartitionNo, partitions.get(0).getPartitionNo())
                    .orderByAsc(CostCalcTaskDetail::getDetailId));
            List<CostCalcTaskDetail> secondPartitionDetails = taskDetailMapper.selectList(Wrappers.<CostCalcTaskDetail>lambdaQuery()
                    .eq(CostCalcTaskDetail::getTaskId, taskId)
                    .eq(CostCalcTaskDetail::getPartitionNo, partitions.get(1).getPartitionNo())
                    .orderByAsc(CostCalcTaskDetail::getDetailId));
            assertThat(firstPartitionDetails).isNotEmpty();
            assertThat(secondPartitionDetails).isNotEmpty();

            System.setProperty("cost.dispatch.node-id", "NODE-A");
            Object firstClaimToken = ReflectionTestUtils.invokeMethod(costRunService,
                    "markPartitionRunning", taskId, firstPartitionDetails);
            System.setProperty("cost.dispatch.node-id", "NODE-B");
            Object secondClaimToken = ReflectionTestUtils.invokeMethod(costRunService,
                    "markPartitionRunning", taskId, secondPartitionDetails);

            Boolean firstOwned = ReflectionTestUtils.invokeMethod(costRunService,
                    "isPartitionClaimOwned", firstClaimToken);
            Boolean secondOwned = ReflectionTestUtils.invokeMethod(costRunService,
                    "isPartitionClaimOwned", secondClaimToken);
            assertThat(firstOwned).isTrue();
            assertThat(secondOwned).isTrue();

            JsonNode overview = readData(mockMvc.perform(get("/cost/run/task/overview")
                            .header("Authorization", authorization)
                            .param("taskId", String.valueOf(taskId))
                            .param("sceneId", String.valueOf(sceneId))
                            .param("billMonth", billMonth))
                    .andExpect(status().isOk())
                    .andReturn());
            JsonNode ownerSummary = overview.path("ownerSummary");
            assertThat(ownerSummary.path("claimedPartitionCount").asInt()).isGreaterThanOrEqualTo(2);
            assertThat(ownerSummary.path("activeOwnerCount").asInt()).isGreaterThanOrEqualTo(2);

            JsonNode nodeA = findNodeByField(overview.path("partitionOwnerDistribution"), "executeNode", "NODE-A");
            JsonNode nodeB = findNodeByField(overview.path("partitionOwnerDistribution"), "executeNode", "NODE-B");
            assertThat(nodeA).isNotNull();
            assertThat(nodeB).isNotNull();
            assertThat(nodeA.path("partitionCount").asInt()).isGreaterThanOrEqualTo(1);
            assertThat(nodeB.path("partitionCount").asInt()).isGreaterThanOrEqualTo(1);

            JsonNode taskDetail = readData(mockMvc.perform(get("/cost/run/task/{taskId}", taskId)
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            JsonNode detailPartitions = taskDetail.path("partitions");
            JsonNode firstPartition = findNodeByField(detailPartitions, "partitionNo",
                    String.valueOf(partitions.get(0).getPartitionNo()));
            JsonNode secondPartition = findNodeByField(detailPartitions, "partitionNo",
                    String.valueOf(partitions.get(1).getPartitionNo()));
            assertThat(firstPartition).isNotNull();
            assertThat(secondPartition).isNotNull();
            assertThat(firstPartition.path("executeNode").asText()).isEqualTo("NODE-A");
            assertThat(secondPartition.path("executeNode").asText()).isEqualTo("NODE-B");
        } finally {
            if (originalNodeId == null) {
                System.clearProperty("cost.dispatch.node-id");
            } else {
                System.setProperty("cost.dispatch.node-id", originalNodeId);
            }
        }
    }

    @Test
    void shouldAllowPeerNodeToAssistRemainingInitPartitionsOfRunningTask() throws Exception {
        String originalNodeId = System.getProperty("cost.dispatch.node-id");
        try {
            Long sceneId = requireSceneId();
            String token = loginAndGetToken();
            String authorization = "Bearer " + token;
            String stamp = LocalTime.now().format(STAMP_FORMATTER);
            Long versionId = publishScene(sceneId, authorization, "partition-assist-running-task-" + stamp);
            String billMonth = YearMonth.now().plusMonths(2).toString();

            ArrayNode inputItems = objectMapper.createArrayNode();
            for (int index = 1; index <= 1501; index++) {
                inputItems.add(createFemaleInputItem(
                        "SG-ASSIST-BIZ-" + stamp + "-" + String.format("%04d", index),
                        "SG-ASSIST-" + stamp + "-" + index,
                        "assist-running-" + index,
                        1,
                        6,
                        6));
            }

            ObjectNode taskBody = objectMapper.createObjectNode();
            taskBody.put("sceneId", sceneId);
            taskBody.put("versionId", versionId);
            taskBody.put("taskType", "FORMAL_BATCH");
            taskBody.put("billMonth", billMonth);
            taskBody.put("requestNo", "SG-ASSIST-REQ-" + stamp);
            taskBody.put("inputJson", objectMapper.writeValueAsString(inputItems));

            JsonNode taskSubmit = readData(mockMvc.perform(post("/cost/run/task/submit")
                            .header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(taskBody)))
                    .andExpect(status().isOk())
                    .andReturn());
            long taskId = taskSubmit.path("task").path("taskId").asLong();
            assertThat(waitTaskFinished(taskId, authorization)).isEqualTo("SUCCESS");

            List<CostCalcTaskPartition> partitions = taskPartitionMapper.selectList(Wrappers.<CostCalcTaskPartition>lambdaQuery()
                    .eq(CostCalcTaskPartition::getTaskId, taskId)
                    .orderByAsc(CostCalcTaskPartition::getPartitionNo));
            assertThat(partitions).hasSizeGreaterThanOrEqualTo(3);

            Date resetTime = DateUtils.getNowDate();
            taskMapper.update(null, Wrappers.<CostCalcTask>lambdaUpdate()
                    .eq(CostCalcTask::getTaskId, taskId)
                    .set(CostCalcTask::getTaskStatus, "RUNNING")
                    .set(CostCalcTask::getSuccessCount, 0)
                    .set(CostCalcTask::getFailCount, 0)
                    .set(CostCalcTask::getProgressPercent, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .set(CostCalcTask::getStartedTime, resetTime)
                    .set(CostCalcTask::getFinishedTime, null)
                    .set(CostCalcTask::getDurationMs, 0L)
                    .set(CostCalcTask::getExecuteNode, "NODE-A")
                    .set(CostCalcTask::getErrorMessage, "")
                    .set(CostCalcTask::getUpdateTime, resetTime));
            taskPartitionMapper.update(null, Wrappers.<CostCalcTaskPartition>lambdaUpdate()
                    .eq(CostCalcTaskPartition::getTaskId, taskId)
                    .set(CostCalcTaskPartition::getPartitionStatus, "INIT")
                    .set(CostCalcTaskPartition::getProcessedCount, 0)
                    .set(CostCalcTaskPartition::getSuccessCount, 0)
                    .set(CostCalcTaskPartition::getFailCount, 0)
                    .set(CostCalcTaskPartition::getAmountTotal, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .set(CostCalcTaskPartition::getExecuteNode, null)
                    .set(CostCalcTaskPartition::getClaimTime, null)
                    .set(CostCalcTaskPartition::getStartedTime, null)
                    .set(CostCalcTaskPartition::getFinishedTime, null)
                    .set(CostCalcTaskPartition::getDurationMs, 0L)
                    .set(CostCalcTaskPartition::getRecoveryHint, "")
                    .set(CostCalcTaskPartition::getLastErrorStage, "")
                    .set(CostCalcTaskPartition::getLastError, "")
                    .set(CostCalcTaskPartition::getUpdateTime, resetTime));
            taskDetailMapper.update(null, Wrappers.<CostCalcTaskDetail>lambdaUpdate()
                    .eq(CostCalcTaskDetail::getTaskId, taskId)
                    .set(CostCalcTaskDetail::getDetailStatus, "INIT")
                    .set(CostCalcTaskDetail::getRetryCount, 0)
                    .set(CostCalcTaskDetail::getResultSummary, "")
                    .set(CostCalcTaskDetail::getErrorMessage, "")
                    .set(CostCalcTaskDetail::getUpdateTime, resetTime));

            List<CostCalcTaskDetail> firstPartitionDetails = taskDetailMapper.selectList(Wrappers.<CostCalcTaskDetail>lambdaQuery()
                    .eq(CostCalcTaskDetail::getTaskId, taskId)
                    .eq(CostCalcTaskDetail::getPartitionNo, partitions.get(0).getPartitionNo())
                    .orderByAsc(CostCalcTaskDetail::getDetailId));
            assertThat(firstPartitionDetails).isNotEmpty();

            System.setProperty("cost.dispatch.node-id", "NODE-A");
            Object firstClaimToken = ReflectionTestUtils.invokeMethod(costRunService,
                    "markPartitionRunning", taskId, firstPartitionDetails);
            Boolean firstOwned = ReflectionTestUtils.invokeMethod(costRunService,
                    "isPartitionClaimOwned", firstClaimToken);
            assertThat(firstOwned).isTrue();

            System.setProperty("cost.dispatch.node-id", "NODE-B");
            ReflectionTestUtils.invokeMethod(costRunService, "dispatchRunnableTaskPartitions");

            boolean assistedByPeerNode = waitPartitionOwnerCount(taskId, "NODE-B", 2, 30_000L);
            assertThat(assistedByPeerNode).isTrue();

            CostCalcTask runningTask = taskMapper.selectById(taskId);
            assertThat(runningTask).isNotNull();
            assertThat(runningTask.getTaskStatus()).isEqualTo("RUNNING");
            assertThat(runningTask.getExecuteNode()).isEqualTo("NODE-A");

            JsonNode overview = readData(mockMvc.perform(get("/cost/run/task/overview")
                            .header("Authorization", authorization)
                            .param("taskId", String.valueOf(taskId))
                            .param("sceneId", String.valueOf(sceneId))
                            .param("billMonth", billMonth))
                    .andExpect(status().isOk())
                    .andReturn());
            JsonNode ownerSummary = overview.path("ownerSummary");
            assertThat(ownerSummary.path("claimedPartitionCount").asInt()).isGreaterThanOrEqualTo(3);
            assertThat(ownerSummary.path("activeOwnerCount").asInt()).isGreaterThanOrEqualTo(2);
            assertThat(findNodeByField(overview.path("partitionOwnerDistribution"), "executeNode", "NODE-A")).isNotNull();
            assertThat(findNodeByField(overview.path("partitionOwnerDistribution"), "executeNode", "NODE-B")).isNotNull();

            System.setProperty("cost.dispatch.node-id", "NODE-A");
            Object snapshot = ReflectionTestUtils.invokeMethod(costRunService,
                    "loadRuntimeSnapshot", sceneId, versionId, true);
            Object firstPartitionResult = ReflectionTestUtils.invokeMethod(costRunService,
                    "executeTaskPartition", taskId, snapshot, firstPartitionDetails, firstClaimToken);
            ReflectionTestUtils.invokeMethod(costRunService,
                    "finishPartition", taskId, firstPartitionDetails, firstClaimToken, firstPartitionResult, null);
            ReflectionTestUtils.invokeMethod(costRunService, "refreshTaskProgress", taskId);
            ReflectionTestUtils.invokeMethod(costRunService, "tryFinalizeTaskIfReady", taskId);

            assertThat(waitTaskFinished(taskId, authorization)).isEqualTo("SUCCESS");

            JsonNode taskDetail = readData(mockMvc.perform(get("/cost/run/task/{taskId}", taskId)
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            JsonNode detailPartitions = taskDetail.path("partitions");
            JsonNode firstPartition = findNodeByField(detailPartitions, "partitionNo",
                    String.valueOf(partitions.get(0).getPartitionNo()));
            JsonNode secondPartition = findNodeByField(detailPartitions, "partitionNo",
                    String.valueOf(partitions.get(1).getPartitionNo()));
            JsonNode thirdPartition = findNodeByField(detailPartitions, "partitionNo",
                    String.valueOf(partitions.get(2).getPartitionNo()));
            assertThat(firstPartition).isNotNull();
            assertThat(secondPartition).isNotNull();
            assertThat(thirdPartition).isNotNull();
            assertThat(firstPartition.path("executeNode").asText()).isEqualTo("NODE-A");
            assertThat(secondPartition.path("executeNode").asText()).isEqualTo("NODE-B");
            assertThat(thirdPartition.path("executeNode").asText()).isEqualTo("NODE-B");
        } finally {
            if (originalNodeId == null) {
                System.clearProperty("cost.dispatch.node-id");
            } else {
                System.setProperty("cost.dispatch.node-id", originalNodeId);
            }
        }
    }

    @Test
    void shouldDispatchMoreThanSingleScanLimitWithinOneRecoveryCoordinatorRound() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "dispatch-round-batch-" + stamp);
        String billMonth = YearMonth.now().plusMonths(2).toString();
        List<Long> taskIds = new ArrayList<>();

        for (int i = 1; i <= 6; i++) {
            ArrayNode inputItems = objectMapper.createArrayNode();
            inputItems.add(createFemaleInputItem(
                    "SG-DISPATCH-BIZ-" + stamp + "-" + String.format("%03d", i),
                    "SG-DISPATCH-" + stamp + "-" + String.format("%03d", i),
                    "dispatch-batch-" + i,
                    1,
                    6,
                    6));

            ObjectNode taskBody = objectMapper.createObjectNode();
            taskBody.put("sceneId", sceneId);
            taskBody.put("versionId", versionId);
            taskBody.put("taskType", "FORMAL_BATCH");
            taskBody.put("billMonth", billMonth);
            taskBody.put("requestNo", "SG-DISPATCH-REQ-" + stamp + "-" + i);
            taskBody.put("inputJson", objectMapper.writeValueAsString(inputItems));

            JsonNode taskSubmit = readData(mockMvc.perform(post("/cost/run/task/submit")
                            .header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(taskBody)))
                    .andExpect(status().isOk())
                    .andReturn());
            long taskId = taskSubmit.path("task").path("taskId").asLong();
            taskIds.add(taskId);
        }

        for (Long taskId : taskIds) {
            assertThat(waitTaskFinished(taskId, authorization)).isEqualTo("SUCCESS");
        }

        Date resetTime = new Date(System.currentTimeMillis() - 2_000L);
        for (Long taskId : taskIds) {
            taskMapper.update(null, Wrappers.<CostCalcTask>lambdaUpdate()
                    .eq(CostCalcTask::getTaskId, taskId)
                    .set(CostCalcTask::getTaskStatus, "INIT")
                    .set(CostCalcTask::getSuccessCount, 0)
                    .set(CostCalcTask::getFailCount, 0)
                    .set(CostCalcTask::getProgressPercent, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .set(CostCalcTask::getStartedTime, null)
                    .set(CostCalcTask::getFinishedTime, null)
                    .set(CostCalcTask::getDurationMs, 0L)
                    .set(CostCalcTask::getExecuteNode, null)
                    .set(CostCalcTask::getErrorMessage, "")
                    .set(CostCalcTask::getUpdateTime, resetTime));
            taskPartitionMapper.update(null, Wrappers.<CostCalcTaskPartition>lambdaUpdate()
                    .eq(CostCalcTaskPartition::getTaskId, taskId)
                    .set(CostCalcTaskPartition::getPartitionStatus, "INIT")
                    .set(CostCalcTaskPartition::getProcessedCount, 0)
                    .set(CostCalcTaskPartition::getSuccessCount, 0)
                    .set(CostCalcTaskPartition::getFailCount, 0)
                    .set(CostCalcTaskPartition::getAmountTotal, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .set(CostCalcTaskPartition::getStartedTime, null)
                    .set(CostCalcTaskPartition::getFinishedTime, null)
                    .set(CostCalcTaskPartition::getDurationMs, 0L)
                    .set(CostCalcTaskPartition::getRecoveryHint, "")
                    .set(CostCalcTaskPartition::getLastErrorStage, "")
                    .set(CostCalcTaskPartition::getLastError, "")
                    .set(CostCalcTaskPartition::getUpdateTime, resetTime));
            taskDetailMapper.update(null, Wrappers.<CostCalcTaskDetail>lambdaUpdate()
                    .eq(CostCalcTaskDetail::getTaskId, taskId)
                    .set(CostCalcTaskDetail::getDetailStatus, "INIT")
                    .set(CostCalcTaskDetail::getRetryCount, 0)
                    .set(CostCalcTaskDetail::getResultSummary, "")
                    .set(CostCalcTaskDetail::getErrorMessage, "")
                    .set(CostCalcTaskDetail::getUpdateTime, resetTime));
        }

        ReflectionTestUtils.invokeMethod(costRunService, "dispatchRecoverableTasksOnce");

        for (Long taskId : taskIds) {
            assertThat(waitTaskFinished(taskId, authorization)).isEqualTo("SUCCESS");
        }

        long remainingInitCount = taskMapper.selectCount(Wrappers.<CostCalcTask>lambdaQuery()
                .in(CostCalcTask::getTaskId, taskIds)
                .eq(CostCalcTask::getTaskStatus, "INIT"));
        assertThat(remainingInitCount).isZero();
    }

    @Test
    void shouldBuildFeeTemplateAndQueryFeeResultDetail() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "结果台账详情与追溯查询回归-" + stamp);
        String billMonth = YearMonth.now().plusMonths(2).toString();

        JsonNode simulationTemplate = readData(mockMvc.perform(get("/cost/run/input-template")
                        .header("Authorization", authorization)
                        .param("sceneId", String.valueOf(sceneId))
                        .param("versionId", String.valueOf(versionId))
                        .param("taskType", "SIMULATION"))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode simulationTemplateInput = objectMapper.readTree(simulationTemplate.path("inputJson").asText("{}"));
        assertThat(simulationTemplateInput.path("objectName").asText()).isEqualTo("\u793a\u4f8b\u5bf9\u8c611");

        JsonNode feeTemplate = readData(mockMvc.perform(get("/cost/run/input-template/fee")
                        .header("Authorization", authorization)
                        .param("sceneId", String.valueOf(sceneId))
                        .param("versionId", String.valueOf(versionId))
                        .param("feeCode", FEMALE_FEE_CODE)
                        .param("taskType", "FORMAL_BATCH"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(feeTemplate.path("fee").path("feeCode").asText()).isEqualTo(FEMALE_FEE_CODE);
        assertThat(feeTemplate.path("inputFieldCount").asInt()).isEqualTo(3);
        assertThat(readIncludedFlags(feeTemplate.path("fields")))
                .containsEntry("FEMALE_TEAM_HEADCOUNT", true)
                .containsEntry("FEMALE_ACTUAL_ATTENDANCE", true)
                .containsEntry("FEMALE_REQUIRED_ATTENDANCE", true)
                .containsEntry("FEMALE_ATTENDANCE_EQUIV", false);

        String requestNo = "SG-FEMALE-" + stamp;
        String objectCode = "SG-FEMALE-" + stamp + "-A";
        ArrayNode inputItems = objectMapper.createArrayNode();
        inputItems.add(createFemaleInputItem("SG-BIZ-" + stamp + "-001", objectCode, "female-sample-a", 2, 6, 6));
        inputItems.add(createFemaleInputItem("SG-BIZ-" + stamp + "-002", "SG-FEMALE-" + stamp + "-B", "female-sample-b", 1, 0, 1));

        ObjectNode feeCalculateBody = objectMapper.createObjectNode();
        feeCalculateBody.put("sceneId", sceneId);
        feeCalculateBody.put("versionId", versionId);
        feeCalculateBody.put("feeCode", FEMALE_FEE_CODE);
        feeCalculateBody.put("billMonth", billMonth);
        feeCalculateBody.put("includeExplain", true);
        feeCalculateBody.put("inputJson", objectMapper.writeValueAsString(inputItems));

        JsonNode feeCalculate = readData(mockMvc.perform(post("/cost/run/fee/calculate")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(feeCalculateBody)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(feeCalculate.path("fee").path("feeCode").asText()).isEqualTo(FEMALE_FEE_CODE);
        assertThat(feeCalculate.path("includeExplain").asBoolean()).isTrue();
        assertThat(feeCalculate.path("recordCount").asInt()).isEqualTo(2);
        assertThat(feeCalculate.path("successCount").asInt()).isEqualTo(2);
        assertThat(feeCalculate.path("failedCount").asInt()).isEqualTo(0);

        JsonNode firstRecord = findNodeByField(feeCalculate.path("records"), "objectCode", objectCode);
        JsonNode secondRecord = findNodeByField(feeCalculate.path("records"), "objectCode", "SG-FEMALE-" + stamp + "-B");
        assertFixedRateRecord(firstRecord, "7233.33");
        assertFixedRateRecord(secondRecord, "0.00");

        ObjectNode taskBody = objectMapper.createObjectNode();
        taskBody.put("sceneId", sceneId);
        taskBody.put("versionId", versionId);
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

        JsonNode resultList = readBody(mockMvc.perform(get("/cost/run/result/list")
                        .header("Authorization", authorization)
                        .param("requestNo", requestNo)
                        .param("feeCode", FEMALE_FEE_CODE)
                        .param("objectCode", objectCode)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(resultList.path("total").asInt()).isGreaterThanOrEqualTo(1);
        JsonNode femaleLedger = findNodeByField(resultList.path("rows"), "objectCode", objectCode);
        assertThat(femaleLedger).isNotNull();
        assertThat(femaleLedger.path("feeCode").asText()).isEqualTo(FEMALE_FEE_CODE);
        assertThat(femaleLedger.path("amountValue").decimalValue()).isEqualByComparingTo("7233.33");
        assertThat(femaleLedger.path("pricingSource").asText()).isEqualTo("FIXED_RATE");

        JsonNode resultDetail = readData(mockMvc.perform(get("/cost/run/result/{resultId}", femaleLedger.path("resultId").asLong())
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(resultDetail.path("ledger").path("feeCode").asText()).isEqualTo(FEMALE_FEE_CODE);
        assertThat(resultDetail.path("trace").path("pricing").path("pricingSource").asText()).isEqualTo("FIXED_RATE");
        assertThat(resultDetail.path("trace").path("pricing").path("amountValue").decimalValue())
                .isEqualByComparingTo("7233.33");
        assertThat(resultDetail.path("trace").path("timeline").isMissingNode()).isFalse();
    }

    @Test
    void shouldPreviewBuiltInputFromRawPayloadMapping() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "input-build-preview-" + stamp);

        ObjectNode rawRow = objectMapper.createObjectNode();
        ObjectNode meta = rawRow.putObject("meta");
        meta.put("bizNo", "SG-PREVIEW-BIZ-" + stamp);
        ObjectNode team = rawRow.putObject("team");
        team.put("code", "SG-PREVIEW-" + stamp);
        team.put("name", "preview-team");
        ObjectNode metrics = rawRow.putObject("metrics");
        ObjectNode female = metrics.putObject("female");
        female.put("headcount", 2);
        female.put("actual", 6);
        female.put("required", 6);

        ObjectNode mapping = objectMapper.createObjectNode();
        mapping.put("bizNo", "meta.bizNo");
        mapping.put("objectCode", "team.code");
        mapping.put("objectName", "team.name");
        mapping.put("FEMALE_TEAM_HEADCOUNT", "metrics.female.headcount");
        mapping.put("FEMALE_ACTUAL_ATTENDANCE", "metrics.female.actual");
        mapping.put("FEMALE_REQUIRED_ATTENDANCE", "metrics.female.required");

        ObjectNode previewBody = objectMapper.createObjectNode();
        previewBody.put("sceneId", sceneId);
        previewBody.put("versionId", versionId);
        previewBody.put("feeCode", FEMALE_FEE_CODE);
        previewBody.put("taskType", "FORMAL_BATCH");
        previewBody.put("rawJson", objectMapper.writeValueAsString(objectMapper.createArrayNode().add(rawRow)));
        previewBody.put("mappingJson", objectMapper.writeValueAsString(mapping));

        JsonNode preview = readData(mockMvc.perform(post("/cost/run/input-build/preview")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(previewBody)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(preview.path("fee").path("feeCode").asText()).isEqualTo(FEMALE_FEE_CODE);
        assertThat(preview.path("rawRecordCount").asInt()).isEqualTo(1);
        assertThat(preview.path("mappedRecordCount").asInt()).isEqualTo(1);
        assertThat(preview.path("missingPaths").isEmpty()).isTrue();
        assertThat(preview.path("loadingGuide").path("estimatedPartitionCount").asInt()).isEqualTo(1);
        assertThat(preview.path("loadingGuide").path("partitionSize").asInt()).isEqualTo(500);
        assertThat(findNodeByField(preview.path("fieldMappings"), "path", "FEMALE_TEAM_HEADCOUNT")
                .path("mappingHint").asText()).isEqualTo("metrics.female.headcount");

        JsonNode mappedRecord = preview.path("mappedRecords").get(0);
        assertThat(mappedRecord.path("bizNo").asText()).isEqualTo("SG-PREVIEW-BIZ-" + stamp);
        assertThat(mappedRecord.path("objectCode").asText()).isEqualTo("SG-PREVIEW-" + stamp);
        assertThat(mappedRecord.path("objectName").asText()).isEqualTo("preview-team");
        assertThat(mappedRecord.path("FEMALE_TEAM_HEADCOUNT").asInt()).isEqualTo(2);
        assertThat(mappedRecord.path("FEMALE_ACTUAL_ATTENDANCE").asInt()).isEqualTo(6);
        assertThat(mappedRecord.path("FEMALE_REQUIRED_ATTENDANCE").asInt()).isEqualTo(6);
    }

    @Test
    void shouldInheritSceneDefaultObjectDimensionWhenFeeDimensionIsBlank() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        CostScene scene = sceneMapper.selectById(sceneId);
        CostFeeItem fee = feeMapper.selectOne(Wrappers.<CostFeeItem>lambdaQuery()
                .eq(CostFeeItem::getSceneId, sceneId)
                .eq(CostFeeItem::getFeeCode, FEMALE_FEE_CODE)
                .last("limit 1"));
        assertThat(scene).isNotNull();
        assertThat(fee).isNotNull();

        String originalSceneDefaultObjectDimension = scene.getDefaultObjectDimension();
        String originalFeeObjectDimension = fee.getObjectDimension();
        String inheritedObjectDimension = "协力队";
        String stamp = LocalTime.now().format(STAMP_FORMATTER);

        sceneMapper.update(null, Wrappers.<CostScene>lambdaUpdate()
                .eq(CostScene::getSceneId, sceneId)
                .set(CostScene::getDefaultObjectDimension, inheritedObjectDimension));
        feeMapper.update(null, Wrappers.<CostFeeItem>lambdaUpdate()
                .eq(CostFeeItem::getFeeId, fee.getFeeId())
                .set(CostFeeItem::getObjectDimension, null));

        try {
            Long versionId = publishScene(sceneId, authorization, "scene-default-object-dimension-" + stamp);

            JsonNode feeTemplate = readData(mockMvc.perform(get("/cost/run/input-template/fee")
                            .header("Authorization", authorization)
                            .param("sceneId", String.valueOf(sceneId))
                            .param("versionId", String.valueOf(versionId))
                            .param("feeCode", FEMALE_FEE_CODE)
                            .param("taskType", "FORMAL_BATCH"))
                    .andExpect(status().isOk())
                    .andReturn());

            assertThat(feeTemplate.path("fee").path("objectDimension").asText()).isEqualTo(inheritedObjectDimension);
            JsonNode feeTemplateInput = objectMapper.readTree(feeTemplate.path("inputJson").asText("{}"));
            JsonNode feeTemplateSample = feeTemplateInput.isArray() ? feeTemplateInput.get(0) : feeTemplateInput;
            assertThat(feeTemplateSample.path("objectDimension").asText()).isEqualTo(inheritedObjectDimension);

            ObjectNode rawRow = objectMapper.createObjectNode();
            ObjectNode meta = rawRow.putObject("meta");
            meta.put("bizNo", "SG-INHERIT-BIZ-" + stamp);
            ObjectNode team = rawRow.putObject("team");
            team.put("code", "SG-INHERIT-" + stamp);
            team.put("name", "inherit-team");
            ObjectNode metrics = rawRow.putObject("metrics");
            ObjectNode female = metrics.putObject("female");
            female.put("headcount", 2);
            female.put("actual", 6);
            female.put("required", 6);

            ObjectNode mapping = objectMapper.createObjectNode();
            mapping.put("bizNo", "meta.bizNo");
            mapping.put("objectCode", "team.code");
            mapping.put("objectName", "team.name");
            mapping.put("FEMALE_TEAM_HEADCOUNT", "metrics.female.headcount");
            mapping.put("FEMALE_ACTUAL_ATTENDANCE", "metrics.female.actual");
            mapping.put("FEMALE_REQUIRED_ATTENDANCE", "metrics.female.required");

            ObjectNode previewBody = objectMapper.createObjectNode();
            previewBody.put("sceneId", sceneId);
            previewBody.put("versionId", versionId);
            previewBody.put("feeCode", FEMALE_FEE_CODE);
            previewBody.put("taskType", "FORMAL_BATCH");
            previewBody.put("rawJson", objectMapper.writeValueAsString(objectMapper.createArrayNode().add(rawRow)));
            previewBody.put("mappingJson", objectMapper.writeValueAsString(mapping));

            JsonNode preview = readData(mockMvc.perform(post("/cost/run/input-build/preview")
                            .header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(previewBody)))
                    .andExpect(status().isOk())
                    .andReturn());

            assertThat(preview.path("fee").path("objectDimension").asText()).isEqualTo(inheritedObjectDimension);
            JsonNode mappedRecord = preview.path("mappedRecords").get(0);
            assertThat(mappedRecord.path("objectDimension").asText()).isEqualTo(inheritedObjectDimension);
        } finally {
            sceneMapper.update(null, Wrappers.<CostScene>lambdaUpdate()
                    .eq(CostScene::getSceneId, sceneId)
                    .set(CostScene::getDefaultObjectDimension, originalSceneDefaultObjectDimension));
            feeMapper.update(null, Wrappers.<CostFeeItem>lambdaUpdate()
                    .eq(CostFeeItem::getFeeId, fee.getFeeId())
                    .set(CostFeeItem::getObjectDimension, originalFeeObjectDimension));
        }
    }

    @Test
    void shouldKeepGroupedPricingConsistentAcrossPublishFeeCalculateAndFormalRun() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "结果台账大表防宽查回归-" + stamp);
        String billMonth = YearMonth.now().plusMonths(2).toString();

        JsonNode publishDetail = readData(mockMvc.perform(get("/cost/publish/{versionId}", versionId)
                        .header("Authorization", authorization)
                        .param("feeCode", COVER_FEE_CODE))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode snapshotRule = findNodeByField(publishDetail.path("snapshotGroups").path("rules"),
                "ruleCode", "SG_COVER_ODD_JOB_GROUPED_01");
        assertThat(snapshotRule).isNotNull();
        assertThat(snapshotRule.path("pricingMode").asText()).isEqualTo("GROUPED");
        assertThat(snapshotRule.path("pricingJson").path("groupPrices").size()).isEqualTo(4);

        JsonNode feeTemplate = readData(mockMvc.perform(get("/cost/run/input-template/fee")
                        .header("Authorization", authorization)
                        .param("sceneId", String.valueOf(sceneId))
                        .param("versionId", String.valueOf(versionId))
                        .param("feeCode", COVER_FEE_CODE)
                        .param("taskType", "FORMAL_BATCH"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(feeTemplate.path("fee").path("feeCode").asText()).isEqualTo(COVER_FEE_CODE);
        assertThat(feeTemplate.path("inputFieldCount").asInt()).isEqualTo(3);
        assertThat(readIncludedFlags(feeTemplate.path("fields")))
                .containsEntry("COVER_ACTION", true)
                .containsEntry("COVER_CARGO_TYPE", true)
                .containsEntry("COVER_WORKLOAD_TON", true);

        String objectCodeA = "SG-COVER-" + stamp + "-A";
        String objectCodeB = "SG-COVER-" + stamp + "-B";
        ArrayNode inputItems = objectMapper.createArrayNode();
        inputItems.add(createCoverInputItem("SG-COVER-BIZ-" + stamp + "-001", objectCodeA, "cover-group-hit-a", "COVER", "COAL", 1000));
        inputItems.add(createCoverInputItem("SG-COVER-BIZ-" + stamp + "-002", objectCodeB, "cover-group-hit-b", "UNCOVER", "ORE", 1000));

        ObjectNode feeCalculateBody = objectMapper.createObjectNode();
        feeCalculateBody.put("sceneId", sceneId);
        feeCalculateBody.put("versionId", versionId);
        feeCalculateBody.put("feeCode", COVER_FEE_CODE);
        feeCalculateBody.put("billMonth", billMonth);
        feeCalculateBody.put("includeExplain", true);
        feeCalculateBody.put("inputJson", objectMapper.writeValueAsString(inputItems));

        JsonNode feeCalculate = readData(mockMvc.perform(post("/cost/run/fee/calculate")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(feeCalculateBody)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(feeCalculate.path("recordCount").asInt()).isEqualTo(2);
        assertThat(feeCalculate.path("successCount").asInt()).isEqualTo(2);
        assertThat(feeCalculate.path("failedCount").asInt()).isEqualTo(0);

        JsonNode feeCalcA = findNodeByField(feeCalculate.path("records"), "objectCode", objectCodeA);
        JsonNode feeCalcB = findNodeByField(feeCalculate.path("records"), "objectCode", objectCodeB);
        assertGroupedPricingRecord(feeCalcA, "1", "16.00");
        assertGroupedPricingRecord(feeCalcB, "4", "9.00");

        String requestNo = "SG-COVER-REQ-" + stamp;
        ObjectNode taskBody = objectMapper.createObjectNode();
        taskBody.put("sceneId", sceneId);
        taskBody.put("versionId", versionId);
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

        JsonNode resultList = readBody(mockMvc.perform(get("/cost/run/result/list")
                        .header("Authorization", authorization)
                        .param("requestNo", requestNo)
                        .param("feeCode", COVER_FEE_CODE)
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(resultList.path("total").asInt()).isGreaterThanOrEqualTo(2);
        JsonNode ledgerA = findNodeByField(resultList.path("rows"), "objectCode", objectCodeA);
        JsonNode ledgerB = findNodeByField(resultList.path("rows"), "objectCode", objectCodeB);
        assertGroupedPricingLedger(ledgerA, "1", "16.00");
        assertGroupedPricingLedger(ledgerB, "4", "9.00");

        JsonNode resultDetail = readData(mockMvc.perform(get("/cost/run/result/{resultId}", ledgerA.path("resultId").asLong())
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(resultDetail.path("ledger").path("feeCode").asText()).isEqualTo(COVER_FEE_CODE);
        assertThat(resultDetail.path("trace").path("pricing").path("pricingMode").asText()).isEqualTo("GROUPED");
        assertThat(resultDetail.path("trace").path("pricing").path("matchedGroupNo").asText()).isEqualTo("1");
        assertThat(resultDetail.path("trace").path("pricing").path("amountValue").decimalValue())
                .isEqualByComparingTo("16.00");
    }

    @Test
    void shouldCalculateFormulaFeeWithUpstreamDependenciesViaSingleFeeApi() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "shougang-management-upstream-" + stamp);
        String billMonth = "2026-12";

        JsonNode feeTemplate = readData(mockMvc.perform(get("/cost/run/input-template/fee")
                        .header("Authorization", authorization)
                        .param("sceneId", String.valueOf(sceneId))
                        .param("versionId", String.valueOf(versionId))
                        .param("feeCode", MANAGEMENT_FEE_CODE)
                        .param("taskType", "FORMAL_BATCH"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(feeTemplate.path("fee").path("feeCode").asText()).isEqualTo(MANAGEMENT_FEE_CODE);
        assertThat(feeTemplate.path("executionFeeCount").asInt()).isEqualTo(11);
        assertThat(readTextSet(feeTemplate.path("dependentFeeCodes")))
                .contains("SG_THRPT_PIECE_FEE", "SG_FEMALE_SHIFT_LABOR", "SG_SPECIAL_SHIFT_LABOR",
                        "SG_HOLD_CLEANING_LABOR", "SG_COVER_ODD_JOB_LABOR", "SG_MOORING_FEE",
                        "SG_ODD_JOB_FEE", "SG_DUTY_SHIFT_LABOR", "SG_SEASONAL_ALLOWANCE", "SG_OVERTIME_FEE");
        assertThat(feeTemplate.path("inputFieldCount").asInt()).isGreaterThanOrEqualTo(19);
        assertThat(readIncludedFlags(feeTemplate.path("fields")))
                .containsEntry("ALLOCATED_THROUGHPUT_TON", true)
                .containsEntry("FEMALE_TEAM_HEADCOUNT", true)
                .containsEntry("SPECIAL_TEAM_HEADCOUNT", true)
                .containsEntry("DUTY_TEAM_REQUIRED_ATTENDANCE", true)
                .containsEntry("COVER_ACTION", true)
                .containsEntry("MOORING_ACTION", true)
                .containsEntry("OVERTIME_DAYS", true);

        ArrayNode inputItems = objectMapper.createArrayNode();
        inputItems.add(createManagementInputItem("SG-MGMT-BIZ-" + stamp + "-001",
                "SG-MGMT-" + stamp + "-A", "management-formula-sample"));

        ObjectNode feeCalculateBody = objectMapper.createObjectNode();
        feeCalculateBody.put("sceneId", sceneId);
        feeCalculateBody.put("versionId", versionId);
        feeCalculateBody.put("feeCode", MANAGEMENT_FEE_CODE);
        feeCalculateBody.put("billMonth", billMonth);
        feeCalculateBody.put("includeExplain", true);
        feeCalculateBody.put("inputJson", objectMapper.writeValueAsString(inputItems));

        JsonNode feeCalculate = readData(mockMvc.perform(post("/cost/run/fee/calculate")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(feeCalculateBody)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(feeCalculate.path("recordCount").asInt()).isEqualTo(1);
        assertThat(feeCalculate.path("successCount").asInt()).isEqualTo(1);
        assertThat(feeCalculate.path("failedCount").asInt()).isEqualTo(0);
        assertThat(feeCalculate.path("executionFeeCount").asInt()).isEqualTo(11);
        assertThat(readTextSet(feeCalculate.path("dependentFeeCodes")))
                .contains("SG_THRPT_PIECE_FEE", "SG_DUTY_SHIFT_LABOR", "SG_SEASONAL_ALLOWANCE");

        JsonNode record = feeCalculate.path("records").path(0);
        assertThat(record.path("status").asText()).isEqualTo("SUCCESS");
        assertThat(record.path("pricingSource").asText()).isEqualTo("FORMULA");
        assertThat(record.path("amountValue").decimalValue()).isEqualByComparingTo(expectedManagementFeeAmount());
        assertThat(record.path("explain").path("pricing").path("pricingSource").asText()).isEqualTo("FORMULA");
        assertThat(record.path("explain").path("pricing").path("formulaCode").asText())
                .isEqualTo("SG_RULE_MANAGEMENT_FEE_AMOUNT");
        assertThat(record.path("explain").path("pricing").path("amountValue").decimalValue())
                .isEqualByComparingTo(expectedManagementFeeAmount());
    }

    @Test
    void shouldKeepShougangRealFeeSampleConsistentAcrossSimulationAndFormalRun() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "shougang-full-sample-" + stamp);
        String billMonth = "2026-12";
        String objectCode = "SG-FULL-" + stamp + "-A";
        String requestNo = "SG-FULL-REQ-" + stamp;

        ObjectNode input = createShougangFullInputItem("SG-FULL-BIZ-" + stamp + "-001", objectCode, "shougang-full-fee-sample");
        Map<String, String> expectedAmounts = expectedShougangFullFeeAmounts();

        ObjectNode simulationBody = objectMapper.createObjectNode();
        simulationBody.put("sceneId", sceneId);
        simulationBody.put("versionId", versionId);
        simulationBody.put("billMonth", billMonth);
        simulationBody.put("inputJson", objectMapper.writeValueAsString(input));

        JsonNode simulation = readData(mockMvc.perform(post("/cost/run/simulation/execute")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(simulationBody)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(simulation.path("record").path("status").asText()).isEqualTo("SUCCESS");
        assertThat(simulation.path("record").path("versionId").asLong()).isEqualTo(versionId);
        assertThat(simulation.path("record").path("billMonth").asText()).isEqualTo(billMonth);
        assertThat(simulation.path("result").path("snapshotSource").asText()).isEqualTo("PUBLISHED");
        assertThat(simulation.path("result").path("feeResults").size()).isEqualTo(expectedAmounts.size());
        assertThat(simulation.path("result").path("amountTotal").decimalValue())
                .isEqualByComparingTo(expectedShougangFullAmountTotal());
        assertFeeAmountMatches(simulation.path("result").path("feeResults"), expectedAmounts);

        ObjectNode taskBody = objectMapper.createObjectNode();
        taskBody.put("sceneId", sceneId);
        taskBody.put("versionId", versionId);
        taskBody.put("taskType", "FORMAL_BATCH");
        taskBody.put("billMonth", billMonth);
        taskBody.put("requestNo", requestNo);
        taskBody.put("inputJson", objectMapper.writeValueAsString(objectMapper.createArrayNode().add(input)));

        JsonNode taskSubmit = readData(mockMvc.perform(post("/cost/run/task/submit")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(taskBody)))
                .andExpect(status().isOk())
                .andReturn());

        long taskId = taskSubmit.path("task").path("taskId").asLong();
        assertThat(waitTaskFinished(taskId, authorization)).isIn("SUCCESS", "PART_SUCCESS");

        JsonNode resultList = readBody(mockMvc.perform(get("/cost/run/result/list")
                        .header("Authorization", authorization)
                        .param("requestNo", requestNo)
                        .param("objectCode", objectCode)
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(resultList.path("total").asInt()).isEqualTo(expectedAmounts.size());
        assertFeeAmountMatches(resultList.path("rows"), expectedAmounts);

        JsonNode managementLedger = findNodeByField(resultList.path("rows"), "feeCode", MANAGEMENT_FEE_CODE);
        assertThat(managementLedger).isNotNull();
        JsonNode managementDetail = readData(mockMvc.perform(get("/cost/run/result/{resultId}", managementLedger.path("resultId").asLong())
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(managementDetail.path("trace").path("pricing").path("formulaCode").asText())
                .isEqualTo("SG_RULE_MANAGEMENT_FEE_AMOUNT");
        assertThat(managementDetail.path("trace").path("pricing").path("amountValue").decimalValue())
                .isEqualByComparingTo(expectedAmounts.get(MANAGEMENT_FEE_CODE));
    }

    @Test
    void shouldFilterSimulationListByBillMonthAndExposePersistedBillMonth() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "simulation-bill-month-filter-" + stamp);
        String billMonth = "2026-12";

        ObjectNode input = createShougangFullInputItem("SG-SIM-BIZ-" + stamp, "SG-SIM-" + stamp, "simulation-bill-month-sample");
        ObjectNode simulationBody = objectMapper.createObjectNode();
        simulationBody.put("sceneId", sceneId);
        simulationBody.put("versionId", versionId);
        simulationBody.put("billMonth", billMonth);
        simulationBody.put("inputJson", objectMapper.writeValueAsString(input));

        JsonNode simulation = readData(mockMvc.perform(post("/cost/run/simulation/execute")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(simulationBody)))
                .andExpect(status().isOk())
                .andReturn());

        long simulationId = simulation.path("record").path("simulationId").asLong();
        String simulationNo = simulation.path("record").path("simulationNo").asText();
        assertThat(simulation.path("record").path("billMonth").asText()).isEqualTo(billMonth);

        JsonNode filteredList = readBody(mockMvc.perform(get("/cost/run/simulation/list")
                        .header("Authorization", authorization)
                        .param("sceneId", String.valueOf(sceneId))
                        .param("versionId", String.valueOf(versionId))
                        .param("billMonth", billMonth)
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode matchedRow = findNodeByField(filteredList.path("rows"), "simulationNo", simulationNo);
        assertThat(matchedRow).isNotNull();
        assertThat(matchedRow.path("billMonth").asText()).isEqualTo(billMonth);

        JsonNode filteredStats = readData(mockMvc.perform(get("/cost/run/simulation/stats")
                        .header("Authorization", authorization)
                        .param("sceneId", String.valueOf(sceneId))
                        .param("versionId", String.valueOf(versionId))
                        .param("billMonth", billMonth))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(filteredStats.path("simulationCount").asInt()).isEqualTo(1);
        assertThat(filteredStats.path("successCount").asInt()).isEqualTo(1);
        assertThat(filteredStats.path("failedCount").asInt()).isEqualTo(0);

        JsonNode otherMonthList = readBody(mockMvc.perform(get("/cost/run/simulation/list")
                        .header("Authorization", authorization)
                        .param("sceneId", String.valueOf(sceneId))
                        .param("versionId", String.valueOf(versionId))
                        .param("billMonth", "2026-11")
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(findNodeByField(otherMonthList.path("rows"), "simulationNo", simulationNo)).isNull();

        JsonNode otherMonthStats = readData(mockMvc.perform(get("/cost/run/simulation/stats")
                        .header("Authorization", authorization)
                        .param("sceneId", String.valueOf(sceneId))
                        .param("versionId", String.valueOf(versionId))
                        .param("billMonth", "2026-11"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(otherMonthStats.path("simulationCount").asInt()).isEqualTo(0);

        JsonNode detail = readData(mockMvc.perform(get("/cost/run/simulation/{simulationId}", simulationId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(detail.path("record").path("simulationNo").asText()).isEqualTo(simulationNo);
        assertThat(detail.path("record").path("billMonth").asText()).isEqualTo(billMonth);
    }

    @Test
    void shouldKeepShougangRealFeeSampleConsistentAcrossAllSingleFeeApis() throws Exception {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "shougang-all-fee-api-" + stamp);
        String billMonth = "2026-12";
        String objectCode = "SG-FEE-API-" + stamp + "-A";

        ArrayNode inputItems = objectMapper.createArrayNode();
        inputItems.add(createShougangFullInputItem("SG-FEE-BIZ-" + stamp + "-001",
                objectCode, "shougang-single-fee-sample"));

        Map<String, String> expectedAmounts = expectedShougangFullFeeAmounts();
        Map<String, Set<String>> expectedFields = expectedShougangSingleFeeTemplateFields();
        Map<String, Integer> expectedInputFieldCounts = expectedShougangSingleFeeTemplateInputFieldCounts();

        for (Map.Entry<String, String> entry : expectedAmounts.entrySet()) {
            String feeCode = entry.getKey();

            JsonNode feeTemplate = readData(mockMvc.perform(get("/cost/run/input-template/fee")
                            .header("Authorization", authorization)
                            .param("sceneId", String.valueOf(sceneId))
                            .param("versionId", String.valueOf(versionId))
                            .param("feeCode", feeCode)
                            .param("taskType", "FORMAL_BATCH"))
                    .andExpect(status().isOk())
                    .andReturn());

            assertThat(feeTemplate.path("fee").path("feeCode").asText()).isEqualTo(feeCode);
            assertThat(feeTemplate.path("inputFieldCount").asInt()).isGreaterThan(0);
            if (expectedInputFieldCounts.containsKey(feeCode)) {
                assertThat(feeTemplate.path("inputFieldCount").asInt()).isEqualTo(expectedInputFieldCounts.get(feeCode));
            }
            Map<String, Boolean> includedFlags = readIncludedFlags(feeTemplate.path("fields"));
            for (String fieldCode : expectedFields.getOrDefault(feeCode, linkedSet())) {
                assertThat(includedFlags).containsEntry(fieldCode, true);
            }
            if (MANAGEMENT_FEE_CODE.equals(feeCode)) {
                assertThat(feeTemplate.path("executionFeeCount").asInt()).isEqualTo(11);
                assertThat(readTextSet(feeTemplate.path("dependentFeeCodes")))
                        .contains("SG_THRPT_PIECE_FEE", "SG_DUTY_SHIFT_LABOR", "SG_SEASONAL_ALLOWANCE");
            }

            ObjectNode feeCalculateBody = objectMapper.createObjectNode();
            feeCalculateBody.put("sceneId", sceneId);
            feeCalculateBody.put("versionId", versionId);
            feeCalculateBody.put("feeCode", feeCode);
            feeCalculateBody.put("billMonth", billMonth);
            feeCalculateBody.put("includeExplain", true);
            feeCalculateBody.put("inputJson", objectMapper.writeValueAsString(inputItems));

            JsonNode feeCalculate = readData(mockMvc.perform(post("/cost/run/fee/calculate")
                            .header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(feeCalculateBody)))
                    .andExpect(status().isOk())
                    .andReturn());

            assertThat(feeCalculate.path("fee").path("feeCode").asText()).isEqualTo(feeCode);
            assertThat(feeCalculate.path("recordCount").asInt()).isEqualTo(1);
            assertThat(feeCalculate.path("successCount").asInt()).isEqualTo(1);
            assertThat(feeCalculate.path("noMatchCount").asInt()).isEqualTo(0);
            assertThat(feeCalculate.path("failedCount").asInt()).isEqualTo(0);

            JsonNode record = feeCalculate.path("records").path(0);
            assertThat(record.path("status").asText()).isEqualTo("SUCCESS");
            assertThat(record.path("amountValue").decimalValue()).isEqualByComparingTo(entry.getValue());
            assertThat(record.path("explain").path("pricing").path("amountValue").decimalValue())
                    .isEqualByComparingTo(entry.getValue());

            if (COVER_FEE_CODE.equals(feeCode)) {
                assertGroupedPricingRecord(record, "1", entry.getValue());
            } else if ("SG_MOORING_FEE".equals(feeCode)) {
                assertGroupedPricingRecord(record, "1", entry.getValue());
            } else if (MANAGEMENT_FEE_CODE.equals(feeCode)) {
                assertThat(record.path("pricingSource").asText()).isEqualTo("FORMULA");
                assertThat(record.path("explain").path("pricing").path("formulaCode").asText())
                        .isEqualTo("SG_RULE_MANAGEMENT_FEE_AMOUNT");
            } else {
                assertThat(record.path("pricingSource").asText()).isNotBlank();
            }
        }
    }

    @Test
    void shouldResolveRemoteVariableFromPublishedSnapshotAndFallbackToLastSnapshot() throws Exception {
        Long sceneId = requireSceneId();
        CostScene scene = sceneMapper.selectById(sceneId);
        assertThat(scene).isNotNull();

        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        String variableCode = "REMOTE_SAMPLE_" + stamp;
        String formulaCode = "REMOTE_FORMULA_" + stamp;
        String feeCode = "REMOTE_FEE_" + stamp;
        String ruleCode = "REMOTE_RULE_" + stamp;
        String objectCode = "REMOTE-OBJ-" + stamp;
        String cacheKey = "cost:runtime:remote:last:" + scene.getSceneCode() + ":ERP:" + variableCode + ":" + objectCode;

        CostVariable variable = new CostVariable();
        variable.setSceneId(sceneId);
        variable.setVariableCode(variableCode);
        variable.setVariableName("远程变量回归_" + stamp);
        variable.setVariableType("NUMBER");
        variable.setSourceType("REMOTE");
        variable.setSourceSystem("ERP");
        variable.setRemoteApi("https://erp.example.com/api/cost");
        variable.setAuthType("NONE");
        variable.setDataPath("value");
        variable.setMappingConfigJson("{\"valuePath\":\"value\"}");
        variable.setSyncMode("REALTIME");
        variable.setCachePolicy("MANUAL_REFRESH");
        variable.setFallbackPolicy("LAST_SNAPSHOT");
        variable.setDataType("NUMBER");
        variable.setDefaultValue("0");
        variable.setStatus("0");
        variable.setSortNo(9910);

        CostFormula formula = new CostFormula();
        formula.setSceneId(sceneId);
        formula.setFormulaCode(formulaCode);
        formula.setFormulaName("远程变量金额公式_" + stamp);
        formula.setFormulaDesc("自动化回归使用");
        formula.setBusinessFormula("远程变量 * 10");
        formula.setFormulaExpr("V." + variableCode + " * 10");
        formula.setAssetType("RULE_AMOUNT");
        formula.setNamespaceScope("SCENE");
        formula.setReturnType("NUMBER");
        formula.setStatus("0");
        formula.setSortNo(9910);

        CostFeeItem fee = new CostFeeItem();
        fee.setSceneId(sceneId);
        fee.setFeeCode(feeCode);
        fee.setFeeName("远程变量验证费用_" + stamp);
        fee.setFeeCategory("REGRESSION");
        fee.setUnitCode("UNIT");
        fee.setFactorSummary("验证 REMOTE 变量真实执行");
        fee.setScopeDescription("自动化回归");
        fee.setObjectDimension("TEAM");
        fee.setSortNo(9910);
        fee.setStatus("0");

        CostRule rule = new CostRule();
        rule.setSceneId(sceneId);
        rule.setRuleCode(ruleCode);
        rule.setRuleName("远程变量金额规则_" + stamp);
        rule.setRuleType("FORMULA");
        rule.setConditionLogic("AND");
        rule.setPriority(99);
        rule.setPricingMode("TYPED");
        rule.setAmountFormula("V." + variableCode + " * 10");
        rule.setAmountFormulaCode(formulaCode);
        rule.setAmountBusinessFormula("远程变量 * 10");
        rule.setStatus("0");
        rule.setSortNo(9910);

        variableMapper.insert(variable);
        formulaMapper.insert(formula);
        feeMapper.insert(fee);
        rule.setFeeId(fee.getFeeId());
        ruleMapper.insert(rule);

        try {
            Long versionId = publishScene(sceneId, authorization, "remote-variable-regression-" + stamp);

            JsonNode feeTemplate = readData(mockMvc.perform(get("/cost/run/input-template/fee")
                            .header("Authorization", authorization)
                            .param("sceneId", String.valueOf(sceneId))
                            .param("versionId", String.valueOf(versionId))
                            .param("feeCode", feeCode)
                            .param("taskType", "SIMULATION"))
                    .andExpect(status().isOk())
                    .andReturn());

            assertThat(feeTemplate.path("fee").path("feeCode").asText()).isEqualTo(feeCode);
            JsonNode remoteField = findNodeByField(feeTemplate.path("fields"), "variableCode", variableCode);
            assertThat(remoteField).isNotNull();
            assertThat(remoteField.path("sourceType").asText()).isEqualTo("REMOTE");
            assertThat(remoteField.path("path").asText()).isEqualTo("remoteContext.ERP." + variableCode + ".value");
            assertThat(remoteField.path("includedInTemplate").asBoolean()).isTrue();

            JsonNode templateInput = objectMapper.readTree(feeTemplate.path("inputJson").asText("{}"));
            assertThat(templateInput.path("remoteContext").path("ERP").path(variableCode).path("value").isMissingNode()).isFalse();

            ObjectNode successInput = objectMapper.createObjectNode();
            successInput.put("bizNo", "REMOTE-BIZ-" + stamp + "-01");
            successInput.put("objectCode", objectCode);
            successInput.put("objectName", "remote-snapshot-success");
            successInput.putObject("remoteContext")
                    .putObject("ERP")
                    .putObject(variableCode)
                    .put("value", 7);

            ArrayNode successItems = objectMapper.createArrayNode().add(successInput);
            ObjectNode feeCalculateBody = objectMapper.createObjectNode();
            feeCalculateBody.put("sceneId", sceneId);
            feeCalculateBody.put("versionId", versionId);
            feeCalculateBody.put("feeCode", feeCode);
            feeCalculateBody.put("includeExplain", true);
            feeCalculateBody.put("inputJson", objectMapper.writeValueAsString(successItems));

            JsonNode firstFeeCalculate = readData(mockMvc.perform(post("/cost/run/fee/calculate")
                            .header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(feeCalculateBody)))
                    .andExpect(status().isOk())
                    .andReturn());

            JsonNode firstRecord = findNodeByField(firstFeeCalculate.path("records"), "objectCode", objectCode);
            assertThat(firstRecord).isNotNull();
            assertThat(firstRecord.path("status").asText()).isEqualTo("SUCCESS");
            assertThat(firstRecord.path("pricingSource").asText()).isEqualTo("FORMULA");
            assertThat(firstRecord.path("amountValue").decimalValue()).isEqualByComparingTo("70.00");
            assertThat(firstRecord.path("explain").path("variables").path(variableCode).decimalValue())
                    .isEqualByComparingTo("7");

            ObjectNode fallbackInput = objectMapper.createObjectNode();
            fallbackInput.put("bizNo", "REMOTE-BIZ-" + stamp + "-02");
            fallbackInput.put("objectCode", objectCode);
            fallbackInput.put("objectName", "remote-snapshot-fallback");

            ArrayNode fallbackItems = objectMapper.createArrayNode().add(fallbackInput);
            feeCalculateBody.put("inputJson", objectMapper.writeValueAsString(fallbackItems));

            JsonNode fallbackFeeCalculate = readData(mockMvc.perform(post("/cost/run/fee/calculate")
                            .header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(feeCalculateBody)))
                    .andExpect(status().isOk())
                    .andReturn());

            JsonNode fallbackRecord = findNodeByField(fallbackFeeCalculate.path("records"), "objectCode", objectCode);
            assertThat(fallbackRecord).isNotNull();
            assertThat(fallbackRecord.path("status").asText()).isEqualTo("SUCCESS");
            assertThat(fallbackRecord.path("amountValue").decimalValue()).isEqualByComparingTo("70.00");
            assertThat(fallbackRecord.path("explain").path("variables").path(variableCode).decimalValue())
                    .isEqualByComparingTo("7");
        } finally {
            redisCache.deleteObject(cacheKey);
            ruleMapper.deleteById(rule.getRuleId());
            feeMapper.deleteById(fee.getFeeId());
            formulaMapper.deleteById(formula.getFormulaId());
            variableMapper.deleteById(variable.getVariableId());
        }
    }

    private Long requireSceneId() {
        CostScene scene = sceneMapper.selectOne(Wrappers.<CostScene>lambdaQuery()
                .eq(CostScene::getSceneCode, SCENE_CODE));
        assertThat(scene).as("missing seeded real-cost scene %s", SCENE_CODE).isNotNull();
        return scene.getSceneId();
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

    private CostFormula requireFormula(Long sceneId, String formulaCode) {
        CostFormula formula = formulaMapper.selectOne(Wrappers.<CostFormula>lambdaQuery()
                .eq(CostFormula::getSceneId, sceneId)
                .eq(CostFormula::getFormulaCode, formulaCode));
        assertThat(formula).as("missing formula %s for scene %s", formulaCode, sceneId).isNotNull();
        return formula;
    }

    private ObjectNode createFemaleInputItem(String bizNo, String objectCode, String objectName,
                                             int teamHeadcount, int actualAttendance, int requiredAttendance) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("bizNo", bizNo);
        item.put("objectCode", objectCode);
        item.put("objectName", objectName);
        item.put("FEMALE_TEAM_HEADCOUNT", teamHeadcount);
        item.put("FEMALE_ACTUAL_ATTENDANCE", actualAttendance);
        item.put("FEMALE_REQUIRED_ATTENDANCE", requiredAttendance);
        return item;
    }

    private ObjectNode createCoverInputItem(String bizNo, String objectCode, String objectName,
                                            String coverAction, String cargoType, int workloadTon) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("bizNo", bizNo);
        item.put("objectCode", objectCode);
        item.put("objectName", objectName);
        item.put("COVER_ACTION", coverAction);
        item.put("COVER_CARGO_TYPE", cargoType);
        item.put("COVER_WORKLOAD_TON", workloadTon);
        return item;
    }

    private ObjectNode createManagementInputItem(String bizNo, String objectCode, String objectName) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("bizNo", bizNo);
        item.put("objectCode", objectCode);
        if (objectName == null) {
            item.putNull("objectName");
        } else {
            item.put("objectName", objectName);
        }
        item.put("ALLOCATED_THROUGHPUT_TON", 100000);
        item.put("FEMALE_TEAM_HEADCOUNT", 2);
        item.put("FEMALE_ACTUAL_ATTENDANCE", 6);
        item.put("FEMALE_REQUIRED_ATTENDANCE", 6);
        item.put("SPECIAL_TEAM_HEADCOUNT", 1);
        item.put("SPECIAL_ACTUAL_ATTENDANCE", 30);
        item.put("SPECIAL_REQUIRED_ATTENDANCE", 30);
        item.put("HOLD_COUNT", 2);
        item.put("COVER_ACTION", "COVER");
        item.put("COVER_CARGO_TYPE", "COAL");
        item.put("COVER_WORKLOAD_TON", 1000);
        item.put("MOORING_ACTION", "MOOR");
        item.put("MOORING_HEADCOUNT", 2);
        item.put("ODD_JOB_HOURS", 10);
        item.put("DUTY_TEAM_ACTUAL_ATTENDANCE", 10);
        item.put("DUTY_TEAM_REQUIRED_ATTENDANCE", 10);
        item.put("ALL_TEAMS_REQUIRED_ATTENDANCE", 20);
        item.put("SEASONAL_SUBSIDY_EQUIV", 2);
        item.put("OVERTIME_DAYS", 3);
        return item;
    }

    private ObjectNode createShougangFullInputItem(String bizNo, String objectCode, String objectName) {
        ObjectNode item = createManagementInputItem(bizNo, objectCode, objectName);
        item.put("UNIT_BEARING_AMOUNT", 3500);
        item.put("INSURANCE_TAXABLE_AMOUNT", 12000);
        item.put("EMPLOYER_LIABILITY_AMOUNT", 1800);
        return item;
    }

    private Map<String, Set<String>> expectedShougangSingleFeeTemplateFields() {
        Map<String, Set<String>> expected = new LinkedHashMap<>();
        expected.put("SG_THRPT_PIECE_FEE", linkedSet("ALLOCATED_THROUGHPUT_TON"));
        expected.put(FEMALE_FEE_CODE, linkedSet("FEMALE_TEAM_HEADCOUNT", "FEMALE_ACTUAL_ATTENDANCE", "FEMALE_REQUIRED_ATTENDANCE"));
        expected.put("SG_SPECIAL_SHIFT_LABOR", linkedSet("SPECIAL_TEAM_HEADCOUNT", "SPECIAL_ACTUAL_ATTENDANCE", "SPECIAL_REQUIRED_ATTENDANCE"));
        expected.put("SG_HOLD_CLEANING_LABOR", linkedSet("HOLD_COUNT"));
        expected.put(COVER_FEE_CODE, linkedSet("COVER_ACTION", "COVER_CARGO_TYPE", "COVER_WORKLOAD_TON"));
        expected.put("SG_MOORING_FEE", linkedSet("MOORING_ACTION", "MOORING_HEADCOUNT"));
        expected.put("SG_ODD_JOB_FEE", linkedSet("ODD_JOB_HOURS"));
        expected.put("SG_DUTY_SHIFT_LABOR", linkedSet("ALLOCATED_THROUGHPUT_TON", "ALL_TEAMS_REQUIRED_ATTENDANCE"));
        expected.put("SG_SEASONAL_ALLOWANCE", linkedSet("SEASONAL_SUBSIDY_EQUIV"));
        expected.put("SG_OVERTIME_FEE", linkedSet("OVERTIME_DAYS"));
        expected.put("SG_UNIT_BEARING_FEE", linkedSet("UNIT_BEARING_AMOUNT"));
        expected.put("SG_INSURANCE_TAXABLE_FEE", linkedSet("INSURANCE_TAXABLE_AMOUNT"));
        expected.put("SG_EMPLOYER_LIABILITY_FEE", linkedSet("EMPLOYER_LIABILITY_AMOUNT"));
        expected.put(MANAGEMENT_FEE_CODE, linkedSet("ALLOCATED_THROUGHPUT_TON", "COVER_ACTION", "OVERTIME_DAYS"));
        return expected;
    }

    private Map<String, Integer> expectedShougangSingleFeeTemplateInputFieldCounts() {
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("SG_THRPT_PIECE_FEE", 1);
        expected.put(FEMALE_FEE_CODE, 3);
        expected.put("SG_SPECIAL_SHIFT_LABOR", 3);
        expected.put("SG_HOLD_CLEANING_LABOR", 1);
        expected.put(COVER_FEE_CODE, 3);
        expected.put("SG_MOORING_FEE", 2);
        expected.put("SG_ODD_JOB_FEE", 1);
        expected.put("SG_SEASONAL_ALLOWANCE", 1);
        expected.put("SG_OVERTIME_FEE", 1);
        expected.put("SG_UNIT_BEARING_FEE", 1);
        expected.put("SG_INSURANCE_TAXABLE_FEE", 1);
        expected.put("SG_EMPLOYER_LIABILITY_FEE", 1);
        return expected;
    }

    private BigDecimal expectedManagementFeeAmount() {
        BigDecimal throughputFee = money(bd("100000").multiply(bd("0.261")));
        BigDecimal femaleFee = money(bd("2.0000").multiply(bd("3616.666667")));
        BigDecimal specialFee = money(bd("1.0000").multiply(bd("8433.333333")));
        BigDecimal holdCleaningFee = money(bd("2").multiply(bd("250")));
        BigDecimal coverFee = money(bd("1000").multiply(bd("0.016")));
        BigDecimal mooringFee = money(bd("2").multiply(bd("16")));
        BigDecimal oddJobFee = money(bd("10").multiply(bd("5")));
        BigDecimal dutyShiftFee = money(
                throughputFee.subtract(femaleFee)
                        .subtract(specialFee)
                        .subtract(holdCleaningFee)
                        .subtract(coverFee)
                        .subtract(mooringFee)
                        .subtract(oddJobFee)
                        .multiply(bd("0.5")));
        BigDecimal seasonalAllowance = money(bd("500").multiply(bd("2")));
        BigDecimal overtimeFee = money(bd("240").multiply(bd("3")));
        return money(throughputFee.add(femaleFee)
                .add(specialFee)
                .add(holdCleaningFee)
                .add(coverFee)
                .add(mooringFee)
                .add(oddJobFee)
                .add(dutyShiftFee)
                .add(seasonalAllowance)
                .add(overtimeFee)
                .multiply(bd("0.1677")));
    }

    private Map<String, String> expectedShougangFullFeeAmounts() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("SG_THRPT_PIECE_FEE", "26100.00");
        expected.put(FEMALE_FEE_CODE, "7233.33");
        expected.put("SG_SPECIAL_SHIFT_LABOR", "8433.33");
        expected.put("SG_HOLD_CLEANING_LABOR", "500.00");
        expected.put(COVER_FEE_CODE, "16.00");
        expected.put("SG_MOORING_FEE", "32.00");
        expected.put("SG_ODD_JOB_FEE", "50.00");
        expected.put("SG_DUTY_SHIFT_LABOR", "4917.67");
        expected.put("SG_SEASONAL_ALLOWANCE", "1000.00");
        expected.put("SG_OVERTIME_FEE", "720.00");
        expected.put("SG_UNIT_BEARING_FEE", "3500.00");
        expected.put("SG_INSURANCE_TAXABLE_FEE", "12000.00");
        expected.put("SG_EMPLOYER_LIABILITY_FEE", "1800.00");
        expected.put(MANAGEMENT_FEE_CODE, expectedManagementFeeAmount().toPlainString());
        return expected;
    }

    private BigDecimal expectedShougangFullAmountTotal() {
        return expectedShougangFullFeeAmounts().values().stream()
                .map(BigDecimal::new)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Set<String> linkedSet(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }

    private Long latestVersionId(Long sceneId) {
        CostPublishVersion latestVersion = publishVersionMapper.selectLatestVersionByScene(sceneId);
        return latestVersion == null ? null : latestVersion.getVersionId();
    }

    private void assertFixedRateRecord(JsonNode record, String amountValue) {
        assertThat(record).isNotNull();
        assertThat(record.path("status").asText()).isEqualTo("SUCCESS");
        assertThat(record.path("pricingSource").asText()).isEqualTo("FIXED_RATE");
        assertThat(record.path("amountValue").decimalValue()).isEqualByComparingTo(amountValue);
        assertThat(record.path("explain").path("pricing").path("pricingSource").asText()).isEqualTo("FIXED_RATE");
        assertThat(record.path("explain").path("pricing").path("amountValue").decimalValue())
                .isEqualByComparingTo(amountValue);
        assertThat(record.path("explain").path("timeline").isArray()).isTrue();
    }

    private void assertGroupedPricingRecord(JsonNode record, String matchedGroupNo, String amountValue) {
        assertThat(record).isNotNull();
        assertThat(record.path("status").asText()).isEqualTo("SUCCESS");
        assertThat(record.path("matchedGroupNo").asText()).isEqualTo(matchedGroupNo);
        assertThat(record.path("pricingMode").asText()).isEqualTo("GROUPED");
        assertThat(record.path("pricingSource").asText()).isEqualTo("FIXED_RATE");
        assertThat(record.path("amountValue").decimalValue()).isEqualByComparingTo(amountValue);
        assertThat(record.path("explain").path("pricing").path("matchedGroupNo").asText()).isEqualTo(matchedGroupNo);
        assertThat(record.path("explain").path("pricing").path("pricingMode").asText()).isEqualTo("GROUPED");
        assertThat(record.path("explain").path("pricing").path("amountValue").decimalValue())
                .isEqualByComparingTo(amountValue);
        assertThat(record.path("explain").path("timeline").isArray()).isTrue();
    }

    private void assertGroupedPricingLedger(JsonNode ledger, String matchedGroupNo, String amountValue) {
        assertThat(ledger).isNotNull();
        assertThat(ledger.path("matchedGroupNo").asText()).isEqualTo(matchedGroupNo);
        assertThat(ledger.path("pricingMode").asText()).isEqualTo("GROUPED");
        assertThat(ledger.path("pricingSource").asText()).isEqualTo("FIXED_RATE");
        assertThat(ledger.path("amountValue").decimalValue()).isEqualByComparingTo(amountValue);
    }

    private void assertFeeAmountMatches(JsonNode items, Map<String, String> expectedAmounts) {
        expectedAmounts.forEach((feeCode, amountValue) -> {
            JsonNode item = findNodeByField(items, "feeCode", feeCode);
            assertThat(item).as("missing fee %s", feeCode).isNotNull();
            assertThat(item.path("amountValue").decimalValue()).isEqualByComparingTo(amountValue);
        });
    }

    private JsonNode findNodeByField(JsonNode items, String fieldName, String expectedValue) {
        if (items == null || !items.isArray()) {
            return null;
        }
        Iterator<JsonNode> iterator = items.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            if (expectedValue.equals(item.path(fieldName).asText())) {
                return item;
            }
        }
        return null;
    }

    private String waitTaskFinished(long taskId, String authorization) throws Exception {
        long deadline = System.currentTimeMillis() + 60_000L;
        String taskStatus = "";
        while (System.currentTimeMillis() < deadline) {
            JsonNode taskDetail = readData(mockMvc.perform(get("/cost/run/task/{taskId}", taskId)
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            taskStatus = taskDetail.path("task").path("taskStatus").asText();
            if (!"INIT".equals(taskStatus) && !"RUNNING".equals(taskStatus)) {
                return taskStatus;
            }
            Thread.sleep(1_000L);
        }
        return taskStatus;
    }

    private String waitTaskStatus(long taskId, String authorization, String expectedStatus) throws Exception {
        long deadline = System.currentTimeMillis() + 60_000L;
        String taskStatus = "";
        while (System.currentTimeMillis() < deadline) {
            JsonNode taskDetail = readData(mockMvc.perform(get("/cost/run/task/{taskId}", taskId)
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            taskStatus = taskDetail.path("task").path("taskStatus").asText();
            if (expectedStatus.equals(taskStatus)) {
                return taskStatus;
            }
            Thread.sleep(1_000L);
        }
        return taskStatus;
    }

    private boolean waitPartitionOwnerCount(long taskId, String executeNode, int minimumCount, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + Math.max(timeoutMs, 1_000L);
        while (System.currentTimeMillis() < deadline) {
            long ownerCount = taskPartitionMapper.selectCount(Wrappers.<CostCalcTaskPartition>lambdaQuery()
                    .eq(CostCalcTaskPartition::getTaskId, taskId)
                    .eq(CostCalcTaskPartition::getExecuteNode, executeNode));
            if (ownerCount >= minimumCount) {
                return true;
            }
            Thread.sleep(500L);
        }
        return false;
    }

    private String resolveExpectedExecuteNode() {
        Object executeNode = ReflectionTestUtils.invokeMethod(costRunService, "resolveExecuteNode");
        return executeNode == null ? "LOCAL" : String.valueOf(executeNode);
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

    private Map<String, Boolean> readIncludedFlags(JsonNode fields) {
        Map<String, Boolean> result = new HashMap<>();
        Iterator<JsonNode> iterator = fields.iterator();
        while (iterator.hasNext()) {
            JsonNode field = iterator.next();
            result.put(field.path("variableCode").asText(), field.path("includedInTemplate").asBoolean());
        }
        return result;
    }

    private Set<String> readTextSet(JsonNode items) {
        Set<String> result = new LinkedHashSet<>();
        if (items == null || !items.isArray()) {
            return result;
        }
        Iterator<JsonNode> iterator = items.iterator();
        while (iterator.hasNext()) {
            result.add(iterator.next().asText());
        }
        return result;
    }

    private Set<String> readFieldSet(JsonNode items, String fieldName) {
        Set<String> result = new LinkedHashSet<>();
        if (items == null || !items.isArray()) {
            return result;
        }
        Iterator<JsonNode> iterator = items.iterator();
        while (iterator.hasNext()) {
            result.add(iterator.next().path(fieldName).asText());
        }
        return result;
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return readBody(result).path("data");
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(content);
    }
}
