package com.ruoyi.system.service.impl.cost;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CostRunPartitionRecoveryManualIT
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
    private CostSceneMapper sceneMapper;

    @Autowired
    private CostPublishVersionMapper publishVersionMapper;

    @SpyBean
    private CostRunServiceImpl costRunService;

    @AfterEach
    void tearDown()
    {
        reset(costRunService);
    }

    @Test
    void shouldFallbackToSinglePersistAndExposeRecoveryHint() throws Exception
    {
        Long sceneId = requireSceneId();
        String token = loginAndGetToken();
        String authorization = "Bearer " + token;
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, authorization, "partition-recovery-fallback-" + stamp);
        String billMonth = YearMonth.now().plusMonths(2).toString();

        doThrow(new RuntimeException("模拟批量落库失败"))
                .when(costRunService).persistPartitionBundleInBatch(anyLong(), any());

        ArrayNode inputItems = objectMapper.createArrayNode();
        inputItems.add(createFemaleInputItem("RECOVER-BIZ-" + stamp + "-001", "RECOVER-" + stamp + "-A", "恢复协力队A", 2, 6, 6));
        inputItems.add(createFemaleInputItem("RECOVER-BIZ-" + stamp + "-002", "RECOVER-" + stamp + "-B", "恢复协力队B", 1, 0, 1));

        ObjectNode taskBody = objectMapper.createObjectNode();
        taskBody.put("sceneId", sceneId);
        taskBody.put("versionId", versionId);
        taskBody.put("taskType", "FORMAL_BATCH");
        taskBody.put("billMonth", billMonth);
        taskBody.put("requestNo", "RECOVER-REQ-" + stamp);
        taskBody.put("inputSourceType", "INLINE_JSON");
        taskBody.put("inputJson", objectMapper.writeValueAsString(inputItems));

        JsonNode submitResult = readData(mockMvc.perform(post("/cost/run/task/submit")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(taskBody)))
                .andExpect(status().isOk())
                .andReturn());

        long taskId = submitResult.path("task").path("taskId").asLong();
        assertThat(waitTaskFinished(taskId, authorization)).isEqualTo("SUCCESS");

        JsonNode taskDetail = readData(mockMvc.perform(get("/cost/run/task/{taskId}", taskId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode partition = taskDetail.path("partitions").get(0);
        assertThat(partition).isNotNull();
        assertThat(partition.path("persistMode").asText()).isEqualTo("SINGLE_FALLBACK");
        assertThat(partition.path("recoveryHint").asText()).contains("批量落库失败后已自动降级为逐条写入");
        assertThat(partition.path("lastErrorStage").asText()).isEqualTo("BATCH_PERSIST");
        assertThat(partition.path("successCount").asInt()).isEqualTo(2);
        assertThat(partition.path("failCount").asInt()).isEqualTo(0);
    }

    private Long requireSceneId()
    {
        CostScene scene = sceneMapper.selectOne(Wrappers.<CostScene>lambdaQuery()
                .eq(CostScene::getSceneCode, SCENE_CODE));
        assertThat(scene).isNotNull();
        return scene.getSceneId();
    }

    private Long publishScene(Long sceneId, String authorization, String publishDesc) throws Exception
    {
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

    private JsonNode readData(MvcResult result) throws Exception
    {
        JsonNode root = readBody(result);
        assertThat(root.path("code").asInt()).isEqualTo(200);
        return root.path("data");
    }

    private JsonNode readBody(MvcResult result) throws Exception
    {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
