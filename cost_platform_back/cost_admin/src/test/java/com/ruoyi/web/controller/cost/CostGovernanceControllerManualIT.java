package com.ruoyi.web.controller.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.cost.CostAlarmRecord;
import com.ruoyi.system.domain.cost.CostFormula;
import com.ruoyi.system.domain.cost.CostPublishVersion;
import com.ruoyi.system.domain.cost.CostScene;
import com.ruoyi.system.mapper.cost.CostFormulaMapper;
import com.ruoyi.system.mapper.cost.CostPublishVersionMapper;
import com.ruoyi.system.mapper.cost.CostSceneMapper;
import com.ruoyi.system.service.cost.ICostAlarmService;
import com.ruoyi.system.service.impl.cost.CostDistributedLockSupport;
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
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CostGovernanceControllerManualIT
{
    private static final DateTimeFormatter STAMP_FORMATTER = DateTimeFormatter.ofPattern("HHmmssSSS");
    private static final String SCENE_CODE = "SHOUGANG-ORE-HR-001";
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
    private CostFormulaMapper formulaMapper;

    @Autowired
    private ICostAlarmService alarmService;

    @Autowired
    private CostDistributedLockSupport distributedLockSupport;

    @Test
    void shouldQueryAckAndResolveTaskAlarmFromGovernanceCenter() throws Exception
    {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        CostFormula formula = requireFormula(sceneId, "SG_RULE_MANAGEMENT_FEE_AMOUNT");
        formula.setFormulaExpr("I.objectName.length()");
        formulaMapper.updateById(formula);
        Long versionId = publishScene(sceneId, authorization, "governance-alarm-regression-" + stamp);

        try
        {
            String billMonth = YearMonth.now().plusMonths(2).toString();
            String requestNo = "SG-ALARM-REQ-" + stamp;

            ObjectNode validInput = createManagementInputItem("SG-ALARM-BIZ-" + stamp + "-001",
                    "SG-ALARM-" + stamp + "-A", "VALID-ALARM");
            ObjectNode failedInput = createManagementInputItem("SG-ALARM-BIZ-" + stamp + "-002",
                    "SG-ALARM-" + stamp + "-B", null);

            ObjectNode taskBody = objectMapper.createObjectNode();
            taskBody.put("sceneId", sceneId);
            taskBody.put("versionId", versionId);
            taskBody.put("taskType", "FORMAL_BATCH");
            taskBody.put("billMonth", billMonth);
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

            JsonNode openAlarmList = readBody(mockMvc.perform(get("/cost/governance/alarm/list")
                            .header("Authorization", authorization)
                            .param("sceneId", String.valueOf(sceneId))
                            .param("billMonth", billMonth)
                            .param("taskId", String.valueOf(taskId))
                            .param("alarmStatus", "OPEN")
                            .param("pageNum", "1")
                            .param("pageSize", "20"))
                    .andExpect(status().isOk())
                    .andReturn());

            assertThat(openAlarmList.path("total").asInt()).isGreaterThanOrEqualTo(2);
            JsonNode detailFailedAlarm = findNodeByField(openAlarmList.path("rows"), "alarmType", "TASK_DETAIL_FAILED");
            JsonNode taskFinishedAlarm = findNodeByField(openAlarmList.path("rows"), "alarmType", "TASK_FINISHED_WITH_ERROR");
            assertThat(detailFailedAlarm).isNotNull();
            assertThat(taskFinishedAlarm).isNotNull();
            assertThat(detailFailedAlarm.path("taskId").asLong()).isEqualTo(taskId);
            assertThat(detailFailedAlarm.path("billMonth").asText()).isEqualTo(billMonth);

            JsonNode stats = readData(mockMvc.perform(get("/cost/governance/alarm/stats")
                            .header("Authorization", authorization)
                            .param("sceneId", String.valueOf(sceneId))
                            .param("billMonth", billMonth)
                            .param("taskId", String.valueOf(taskId)))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(stats.path("alarmCount").asInt()).isGreaterThanOrEqualTo(2);
            assertThat(stats.path("openCount").asInt()).isGreaterThanOrEqualTo(2);

            JsonNode overview = readData(mockMvc.perform(get("/cost/governance/alarm/overview")
                            .header("Authorization", authorization)
                            .param("sceneId", String.valueOf(sceneId))
                            .param("billMonth", billMonth)
                            .param("taskId", String.valueOf(taskId)))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(overview.path("recentTrend").isArray()).isTrue();
            assertThat(overview.path("topAlarmTypes").isArray()).isTrue();
            JsonNode topTask = findNodeByField(overview.path("topTasks"), "taskId", String.valueOf(taskId));
            assertThat(topTask).isNotNull();
            assertThat(topTask.path("latestTitle").asText()).contains("正式核算任务部分成功");

            long alarmId = detailFailedAlarm.path("alarmId").asLong();
            JsonNode ackResponse = readBody(mockMvc.perform(put("/cost/governance/alarm/ack/{alarmId}", alarmId)
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(ackResponse.path("code").asInt()).isEqualTo(200);

            JsonNode ackedAlarmList = readBody(mockMvc.perform(get("/cost/governance/alarm/list")
                            .header("Authorization", authorization)
                            .param("sceneId", String.valueOf(sceneId))
                            .param("billMonth", billMonth)
                            .param("taskId", String.valueOf(taskId))
                            .param("alarmStatus", "ACKED")
                            .param("pageNum", "1")
                            .param("pageSize", "20"))
                    .andExpect(status().isOk())
                    .andReturn());
            JsonNode ackedAlarm = findNodeByField(ackedAlarmList.path("rows"), "alarmId", String.valueOf(alarmId));
            assertThat(ackedAlarm).isNotNull();
            assertThat(ackedAlarm.path("alarmStatus").asText()).isEqualTo("ACKED");

            JsonNode resolveResponse = readBody(mockMvc.perform(put("/cost/governance/alarm/resolve/{alarmId}", alarmId)
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(resolveResponse.path("code").asInt()).isEqualTo(200);

            JsonNode resolvedAlarmList = readBody(mockMvc.perform(get("/cost/governance/alarm/list")
                            .header("Authorization", authorization)
                            .param("sceneId", String.valueOf(sceneId))
                            .param("billMonth", billMonth)
                            .param("taskId", String.valueOf(taskId))
                            .param("alarmStatus", "RESOLVED")
                            .param("pageNum", "1")
                            .param("pageSize", "20"))
                    .andExpect(status().isOk())
                    .andReturn());
            JsonNode resolvedAlarm = findNodeByField(resolvedAlarmList.path("rows"), "alarmId", String.valueOf(alarmId));
            assertThat(resolvedAlarm).isNotNull();
            assertThat(resolvedAlarm.path("alarmStatus").asText()).isEqualTo("RESOLVED");
        }
        finally
        {
            formula.setFormulaExpr(MANAGEMENT_FEE_FORMULA_BASELINE);
            formulaMapper.updateById(formula);
        }
    }

    @Test
    void shouldExposeAndRefreshRuntimeCacheStats() throws Exception
    {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "governance-cache-regression-" + stamp);

        JsonNode initialRefresh = readBody(mockMvc.perform(put("/cost/governance/cache/refresh")
                        .header("Authorization", authorization)
                        .param("versionId", String.valueOf(versionId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(initialRefresh.path("code").asInt()).isEqualTo(200);

        JsonNode emptyCacheStats = readData(mockMvc.perform(get("/cost/governance/cache/stats")
                        .header("Authorization", authorization)
                        .param("versionId", String.valueOf(versionId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(emptyCacheStats.path("exists").asBoolean()).isFalse();

        JsonNode template = readData(mockMvc.perform(get("/cost/run/input-template")
                        .header("Authorization", authorization)
                        .param("sceneId", String.valueOf(sceneId))
                        .param("versionId", String.valueOf(versionId))
                        .param("taskType", "SIMULATION"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(template.path("versionId").asLong()).isEqualTo(versionId);
        assertThat(template.path("snapshotSource").asText()).isEqualTo("PUBLISHED");

        JsonNode filledCacheStats = readData(mockMvc.perform(get("/cost/governance/cache/stats")
                        .header("Authorization", authorization)
                        .param("sceneId", String.valueOf(sceneId))
                        .param("versionId", String.valueOf(versionId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(filledCacheStats.path("exists").asBoolean()).isTrue();
        assertThat(filledCacheStats.path("cacheKey").asText()).isEqualTo("cost:runtime:snapshot:" + versionId);
        assertThat(filledCacheStats.path("cacheKeys").toString()).contains("cost:runtime:snapshot:" + versionId);
        assertThat(filledCacheStats.path("expireSeconds").asLong()).isGreaterThan(0L);

        JsonNode refreshResponse = readBody(mockMvc.perform(put("/cost/governance/cache/refresh")
                        .header("Authorization", authorization)
                        .param("sceneId", String.valueOf(sceneId))
                        .param("versionId", String.valueOf(versionId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(refreshResponse.path("code").asInt()).isEqualTo(200);

        JsonNode refreshedCacheStats = readData(mockMvc.perform(get("/cost/governance/cache/stats")
                        .header("Authorization", authorization)
                        .param("versionId", String.valueOf(versionId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(refreshedCacheStats.path("exists").asBoolean()).isFalse();
    }

    @Test
    void shouldBlockRuntimeCacheRefreshWhenRefreshLockExists() throws Exception
    {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "cache-lock-baseline-" + stamp);
        String lockKey = distributedLockSupport.buildRuntimeCacheLockKey(null, versionId);
        redisCache.setCacheObject(lockKey, "itest-lock", 30, TimeUnit.SECONDS);

        try
        {
            JsonNode response = readBody(mockMvc.perform(put("/cost/governance/cache/refresh")
                            .header("Authorization", authorization)
                            .param("versionId", String.valueOf(versionId)))
                    .andExpect(status().isOk())
                    .andReturn());
            assertThat(response.path("code").asInt()).isEqualTo(500);
            assertThat(response.path("msg").asText()).contains("运行缓存");
        }
        finally
        {
            redisCache.deleteObject(lockKey);
        }
    }

    @Test
    void shouldAutoResolveCacheAlarmAfterRefreshRuntimeCache() throws Exception
    {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "governance-cache-heal-" + stamp);

        CostAlarmRecord alarm = new CostAlarmRecord();
        alarm.setSceneId(sceneId);
        alarm.setVersionId(versionId);
        alarm.setAlarmType("CACHE_REFRESH_FAILED");
        alarm.setAlarmLevel("ERROR");
        alarm.setAlarmTitle("运行快照缓存刷新失败");
        alarm.setAlarmContent("用于验证缓存刷新成功后的自动自愈。");
        alarm.setSourceKey("CACHE:" + versionId);
        alarmService.createAlarm(alarm);

        JsonNode openAlarmList = readBody(mockMvc.perform(get("/cost/governance/alarm/list")
                        .header("Authorization", authorization)
                        .param("sceneId", String.valueOf(sceneId))
                        .param("alarmStatus", "OPEN")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode cacheAlarm = findNodeByField(openAlarmList.path("rows"), "sourceKey", "CACHE:" + versionId);
        assertThat(cacheAlarm).isNotNull();

        JsonNode refreshResponse = readBody(mockMvc.perform(put("/cost/governance/cache/refresh")
                        .header("Authorization", authorization)
                        .param("sceneId", String.valueOf(sceneId))
                        .param("versionId", String.valueOf(versionId)))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(refreshResponse.path("code").asInt()).isEqualTo(200);

        JsonNode resolvedAlarmList = readBody(mockMvc.perform(get("/cost/governance/alarm/list")
                        .header("Authorization", authorization)
                        .param("sceneId", String.valueOf(sceneId))
                        .param("alarmStatus", "RESOLVED")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode resolvedAlarm = findNodeByField(resolvedAlarmList.path("rows"), "sourceKey", "CACHE:" + versionId);
        assertThat(resolvedAlarm).isNotNull();
        assertThat(resolvedAlarm.path("alarmStatus").asText()).isEqualTo("RESOLVED");
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

    private Long latestVersionId(Long sceneId)
    {
        CostPublishVersion latestVersion = publishVersionMapper.selectLatestVersionByScene(sceneId);
        return latestVersion == null ? null : latestVersion.getVersionId();
    }

    private CostFormula requireFormula(Long sceneId, String formulaCode)
    {
        CostFormula formula = formulaMapper.selectOne(Wrappers.<CostFormula>lambdaQuery()
                .eq(CostFormula::getSceneId, sceneId)
                .eq(CostFormula::getFormulaCode, formulaCode));
        assertThat(formula).as("missing formula %s for scene %s", formulaCode, sceneId).isNotNull();
        return formula;
    }

    private ObjectNode createManagementInputItem(String bizNo, String objectCode, String objectName)
    {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("bizNo", bizNo);
        item.put("objectCode", objectCode);
        if (objectName == null)
        {
            item.putNull("objectName");
        }
        else
        {
            item.put("objectName", objectName);
        }
        item.put("ALLOCATED_THROUGHPUT_TON", 100000);
        item.put("FEMALE_TEAM_HEADCOUNT", 2);
        item.put("FEMALE_ACTUAL_ATTENDANCE", 6);
        item.put("FEMALE_REQUIRED_ATTENDANCE", 6);
        item.put("SPECIAL_TEAM_HEADCOUNT", 2);
        item.put("SPECIAL_ACTUAL_ATTENDANCE", 6);
        item.put("SPECIAL_REQUIRED_ATTENDANCE", 6);
        item.put("DUTY_TEAM_ACTUAL_ATTENDANCE", 6);
        item.put("DUTY_TEAM_REQUIRED_ATTENDANCE", 6);
        item.put("ALL_TEAMS_REQUIRED_ATTENDANCE", 12);
        item.put("SEASONAL_SUBSIDY_EQUIV", 3.5);
        item.put("OVERTIME_DAYS", 4);
        item.put("UNIT_BEARING_AMOUNT", 3500);
        item.put("INSURANCE_TAXABLE_AMOUNT", 12000);
        item.put("EMPLOYER_LIABILITY_AMOUNT", 1800);
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
