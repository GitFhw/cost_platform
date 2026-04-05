package com.ruoyi.web.controller.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.cost.CostPublishVersion;
import com.ruoyi.system.domain.cost.CostScene;
import com.ruoyi.system.mapper.cost.CostPublishVersionMapper;
import com.ruoyi.system.mapper.cost.CostSceneMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CostRunControllerManualIT
{
    private static final DateTimeFormatter STAMP_FORMATTER = DateTimeFormatter.ofPattern("HHmmssSSS");
    private static final String SCENE_CODE = "SHOUGANG-ORE-HR-001";
    private static final String FEMALE_FEE_CODE = "SG_FEMALE_SHIFT_LABOR";
    private static final String COVER_FEE_CODE = "SG_COVER_ODD_JOB_LABOR";
    private static final String MANAGEMENT_FEE_CODE = "SG_MANAGEMENT_FEE";

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

    @Test
    void shouldUseDraftConfigWhenSimulationVersionIsNotSpecified() throws Exception
    {
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

        try
        {
            JsonNode template = readData(mockMvc.perform(get("/cost/run/input-template")
                            .header("Authorization", authorization)
                            .param("sceneId", String.valueOf(sceneId))
                            .param("taskType", "SIMULATION"))
                    .andExpect(status().isOk())
                    .andReturn());

            assertThat(template.path("versionId").isNull()).isTrue();
            assertThat(template.path("versionNo").asText()).isEqualTo("草稿配置");
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
            assertThat(simulation.path("record").path("versionNo").asText()).isEqualTo("草稿配置");
            assertThat(simulation.path("result").path("versionNo").asText()).isEqualTo("草稿配置");
            assertThat(simulation.path("result").path("snapshotSource").asText()).isEqualTo("DRAFT");
        }
        finally
        {
            sceneMapper.update(null, Wrappers.<CostScene>lambdaUpdate()
                    .eq(CostScene::getSceneId, sceneId)
                    .set(CostScene::getActiveVersionId, previousActiveVersionId));
        }
    }

    @Test
    void shouldBuildFeeTemplateAndQueryFeeResultDetail() throws Exception
    {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "首钢真实费用-女工联调-" + stamp);
        String billMonth = YearMonth.now().plusMonths(2).toString();

        JsonNode simulationTemplate = readData(mockMvc.perform(get("/cost/run/input-template")
                        .header("Authorization", authorization)
                        .param("sceneId", String.valueOf(sceneId))
                        .param("versionId", String.valueOf(versionId))
                        .param("taskType", "SIMULATION"))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode simulationTemplateInput = objectMapper.readTree(simulationTemplate.path("inputJson").asText("{}"));
        assertThat(simulationTemplateInput.path("objectName").asText()).isEqualTo("示例对象1");

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
        inputItems.add(createFemaleInputItem("SG-BIZ-" + stamp + "-001", objectCode, "首钢女工A", 2, 6, 6));
        inputItems.add(createFemaleInputItem("SG-BIZ-" + stamp + "-002", "SG-FEMALE-" + stamp + "-B", "首钢女工B", 1, 0, 1));

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
    void shouldKeepGroupedPricingConsistentAcrossPublishFeeCalculateAndFormalRun() throws Exception
    {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "首钢真实费用-组合定价联调-" + stamp);
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
        inputItems.add(createCoverInputItem("SG-COVER-BIZ-" + stamp + "-001", objectCodeA, "首钢苫盖焦煤", "COVER", "COAL", 1000));
        inputItems.add(createCoverInputItem("SG-COVER-BIZ-" + stamp + "-002", objectCodeB, "首钢揭盖矿石", "UNCOVER", "ORE", 1000));

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
    void shouldCalculateFormulaFeeWithUpstreamDependenciesViaSingleFeeApi() throws Exception
    {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "首钢真实费用-管理费上游联调-" + stamp);
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
                "SG-MGMT-" + stamp + "-A", "首钢管理费联动样例"));

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

    private Long requireSceneId()
    {
        CostScene scene = sceneMapper.selectOne(Wrappers.<CostScene>lambdaQuery()
                .eq(CostScene::getSceneCode, SCENE_CODE));
        assertThat(scene).as("missing seeded real-cost scene %s", SCENE_CODE).isNotNull();
        return scene.getSceneId();
    }

    private Long publishScene(Long sceneId, String authorization, String publishDesc) throws Exception
    {
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
        if (previousLatestVersionId != null)
        {
            assertThat(latestVersion.getVersionId()).isNotEqualTo(previousLatestVersionId);
        }
        return latestVersion.getVersionId();
    }

    private ObjectNode createFemaleInputItem(String bizNo, String objectCode, String objectName,
                                             int teamHeadcount, int actualAttendance, int requiredAttendance)
    {
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
                                            String coverAction, String cargoType, int workloadTon)
    {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("bizNo", bizNo);
        item.put("objectCode", objectCode);
        item.put("objectName", objectName);
        item.put("COVER_ACTION", coverAction);
        item.put("COVER_CARGO_TYPE", cargoType);
        item.put("COVER_WORKLOAD_TON", workloadTon);
        return item;
    }

    private ObjectNode createManagementInputItem(String bizNo, String objectCode, String objectName)
    {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("bizNo", bizNo);
        item.put("objectCode", objectCode);
        item.put("objectName", objectName);
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

    private BigDecimal expectedManagementFeeAmount()
    {
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

    private BigDecimal bd(String value)
    {
        return new BigDecimal(value);
    }

    private BigDecimal money(BigDecimal value)
    {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Long latestVersionId(Long sceneId)
    {
        CostPublishVersion latestVersion = publishVersionMapper.selectLatestVersionByScene(sceneId);
        return latestVersion == null ? null : latestVersion.getVersionId();
    }

    private void assertFixedRateRecord(JsonNode record, String amountValue)
    {
        assertThat(record).isNotNull();
        assertThat(record.path("status").asText()).isEqualTo("SUCCESS");
        assertThat(record.path("pricingSource").asText()).isEqualTo("FIXED_RATE");
        assertThat(record.path("amountValue").decimalValue()).isEqualByComparingTo(amountValue);
        assertThat(record.path("explain").path("pricing").path("pricingSource").asText()).isEqualTo("FIXED_RATE");
        assertThat(record.path("explain").path("pricing").path("amountValue").decimalValue())
                .isEqualByComparingTo(amountValue);
        assertThat(record.path("explain").path("timeline").isArray()).isTrue();
    }

    private void assertGroupedPricingRecord(JsonNode record, String matchedGroupNo, String amountValue)
    {
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

    private void assertGroupedPricingLedger(JsonNode ledger, String matchedGroupNo, String amountValue)
    {
        assertThat(ledger).isNotNull();
        assertThat(ledger.path("matchedGroupNo").asText()).isEqualTo(matchedGroupNo);
        assertThat(ledger.path("pricingMode").asText()).isEqualTo("GROUPED");
        assertThat(ledger.path("pricingSource").asText()).isEqualTo("FIXED_RATE");
        assertThat(ledger.path("amountValue").decimalValue()).isEqualByComparingTo(amountValue);
    }

    private JsonNode findNodeByField(JsonNode items, String fieldName, String expectedValue)
    {
        if (items == null || !items.isArray())
        {
            return null;
        }
        Iterator<JsonNode> iterator = items.iterator();
        while (iterator.hasNext())
        {
            JsonNode item = iterator.next();
            if (expectedValue.equals(item.path(fieldName).asText()))
            {
                return item;
            }
        }
        return null;
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

    private Set<String> readTextSet(JsonNode items)
    {
        Set<String> result = new LinkedHashSet<>();
        if (items == null || !items.isArray())
        {
            return result;
        }
        Iterator<JsonNode> iterator = items.iterator();
        while (iterator.hasNext())
        {
            result.add(iterator.next().asText());
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
