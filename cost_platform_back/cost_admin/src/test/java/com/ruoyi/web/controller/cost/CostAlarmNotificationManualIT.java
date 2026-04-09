package com.ruoyi.web.controller.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.domain.cost.CostAlarmRecord;
import com.ruoyi.system.domain.cost.CostScene;
import com.ruoyi.system.mapper.SysConfigMapper;
import com.ruoyi.system.mapper.cost.CostAlarmRecordMapper;
import com.ruoyi.system.mapper.cost.CostSceneMapper;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.cost.ICostAlarmService;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CostAlarmNotificationManualIT {
    private static final String SCENE_CODE = "SHOUGANG-ORE-HR-001";
    private static final String WEBHOOK_ENABLED_KEY = "cost.alarm.webhook.enabled";
    private static final String WEBHOOK_URL_KEY = "cost.alarm.webhook.url";
    private static final String WEBHOOK_HEADERS_KEY = "cost.alarm.webhook.headers";
    private static final String WEBHOOK_SECRET_KEY = "cost.alarm.webhook.secret";
    private static final DateTimeFormatter STAMP_FORMATTER = DateTimeFormatter.ofPattern("HHmmssSSS");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private RestTemplate costAccessRestTemplate;

    @Autowired
    private ICostAlarmService alarmService;

    @Autowired
    private CostSceneMapper sceneMapper;

    @Autowired
    private CostAlarmRecordMapper alarmRecordMapper;

    @Autowired
    private SysConfigMapper sysConfigMapper;

    @Autowired
    private ISysConfigService configService;

    @Test
    void shouldSendWebhookAndExposeNotificationSummary() throws Exception {
        Long sceneId = requireSceneId();
        String sceneName = requireSceneName(sceneId);
        String token = loginAndGetToken();
        Map<String, ConfigSnapshot> snapshots = snapshotConfigs(
                WEBHOOK_ENABLED_KEY,
                WEBHOOK_URL_KEY,
                WEBHOOK_HEADERS_KEY,
                WEBHOOK_SECRET_KEY
        );
        MockRestServiceServer server = MockRestServiceServer.bindTo(costAccessRestTemplate).build();
        server.expect(once(), requestTo("https://example.internal/alarm-webhook"))
                .andExpect(method(POST))
                .andExpect(header("X-App", "cost-platform"))
                .andExpect(header("X-Cost-Alarm-Secret", "demo-secret"))
                .andExpect(content().json("""
                        {
                          "sceneId": %d,
                          "sceneCode": "%s",
                          "sceneName": "%s",
                          "alarmTitle": "测试外部通知",
                          "alarmType": "TASK_FINISHED_WITH_ERROR",
                          "platform": "cost_platform"
                        }
                        """.formatted(sceneId, SCENE_CODE, sceneName), false))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        try {
            upsertConfig("成本告警-WebHook通知开关", WEBHOOK_ENABLED_KEY, "true", "测试启用 Webhook");
            upsertConfig("成本告警-WebHook地址", WEBHOOK_URL_KEY, "https://example.internal/alarm-webhook", "测试 Webhook 地址");
            upsertConfig("成本告警-WebHook扩展头", WEBHOOK_HEADERS_KEY, "{\"X-App\":\"cost-platform\"}", "测试扩展头");
            upsertConfig("成本告警-WebHook签名密钥", WEBHOOK_SECRET_KEY, "demo-secret", "测试签名密钥");
            refreshInsertedConfigIds(snapshots);
            configService.resetConfigCache();

            CostAlarmRecord alarmRecord = new CostAlarmRecord();
            alarmRecord.setSceneId(sceneId);
            alarmRecord.setTaskId(999999L);
            alarmRecord.setBillMonth("2026-04");
            alarmRecord.setAlarmType("TASK_FINISHED_WITH_ERROR");
            alarmRecord.setAlarmLevel("ERROR");
            alarmRecord.setAlarmTitle("测试外部通知");
            alarmRecord.setAlarmContent("用于验证 Webhook 外部通知与告警中心摘要。");
            alarmRecord.setSourceKey("ALARM-WEBHOOK-IT");

            alarmService.createAlarm(alarmRecord);
            server.verify();

            JsonNode overview = readData(mockMvc.perform(get("/cost/governance/alarm/overview")
                            .header("Authorization", "Bearer " + token)
                            .param("sceneId", String.valueOf(sceneId)))
                    .andExpect(status().isOk())
                    .andReturn());

            JsonNode summary = overview.path("notificationSummary");
            assertThat(summary.path("enabled").asBoolean()).isTrue();
            assertThat(summary.path("configured").asBoolean()).isTrue();
            assertThat(summary.path("headersConfigured").asBoolean()).isTrue();
            assertThat(summary.path("secretConfigured").asBoolean()).isTrue();
            assertThat(summary.path("channelType").asText()).isEqualTo("WEBHOOK");
            assertThat(summary.path("target").asText()).contains("example.");
        } finally {
            restoreConfigs(snapshots);
            configService.resetConfigCache();
        }
    }

    @Test
    void shouldAggregateEscalateAndResolveAlarmBySourceKey() {
        Long sceneId = requireSceneId();
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        String sourceKey = "ALARM-AGG-" + stamp;

        for (int i = 1; i <= 3; i++) {
            CostAlarmRecord alarmRecord = new CostAlarmRecord();
            alarmRecord.setSceneId(sceneId);
            alarmRecord.setAlarmType("CACHE_REFRESH_FAILED");
            alarmRecord.setAlarmLevel("WARN");
            alarmRecord.setAlarmTitle("缓存刷新失败");
            alarmRecord.setAlarmContent("第 " + i + " 次命中缓存刷新异常");
            alarmRecord.setSourceKey(sourceKey);
            alarmService.createAlarm(alarmRecord);
        }

        CostAlarmRecord aggregated = alarmRecordMapper.selectOne(Wrappers.<CostAlarmRecord>lambdaQuery()
                .eq(CostAlarmRecord::getSourceKey, sourceKey));
        assertThat(aggregated).isNotNull();
        assertThat(aggregated.getOccurrenceCount()).isEqualTo(3);
        assertThat(aggregated.getAlarmStatus()).isEqualTo("OPEN");
        assertThat(aggregated.getAlarmLevel()).isEqualTo("ERROR");
        assertThat(aggregated.getFirstTriggerTime()).isNotNull();
        assertThat(aggregated.getLatestTriggerTime()).isNotNull();

        int affected = alarmService.autoResolveBySourceKey(sourceKey, "缓存恢复，自动关闭聚合告警");
        assertThat(affected).isEqualTo(1);

        CostAlarmRecord resolved = alarmRecordMapper.selectById(aggregated.getAlarmId());
        assertThat(resolved.getAlarmStatus()).isEqualTo("RESOLVED");
        assertThat(resolved.getResolveBy()).isEqualTo("system");
        assertThat(resolved.getRemark()).contains("缓存恢复");
    }

    private Long requireSceneId() {
        CostScene scene = sceneMapper.selectOne(Wrappers.<CostScene>lambdaQuery()
                .eq(CostScene::getSceneCode, SCENE_CODE));
        assertThat(scene).as("missing seeded scene %s", SCENE_CODE).isNotNull();
        return scene.getSceneId();
    }

    private String requireSceneName(Long sceneId) {
        CostScene scene = sceneMapper.selectById(sceneId);
        assertThat(scene).isNotNull();
        return scene.getSceneName();
    }

    private Map<String, ConfigSnapshot> snapshotConfigs(String... keys) {
        LinkedHashMap<String, ConfigSnapshot> snapshots = new LinkedHashMap<>();
        for (String key : keys) {
            SysConfig existing = sysConfigMapper.checkConfigKeyUnique(key);
            snapshots.put(key, new ConfigSnapshot(existing));
        }
        return snapshots;
    }

    private void restoreConfigs(Map<String, ConfigSnapshot> snapshots) {
        snapshots.forEach((key, snapshot) -> {
            if (snapshot.exists()) {
                SysConfig config = snapshot.toConfig();
                config.setUpdateBy("admin");
                sysConfigMapper.updateConfig(config);
            } else if (snapshot.getInsertedConfigId() != null) {
                sysConfigMapper.deleteConfigById(snapshot.getInsertedConfigId());
            }
        });
    }

    private void refreshInsertedConfigIds(Map<String, ConfigSnapshot> snapshots) {
        snapshots.forEach((key, snapshot) -> {
            if (!snapshot.exists() && snapshot.getInsertedConfigId() == null) {
                SysConfig inserted = sysConfigMapper.checkConfigKeyUnique(key);
                if (inserted != null) {
                    snapshot.setInsertedConfigId(inserted.getConfigId());
                }
            }
        });
    }

    private void upsertConfig(String name, String key, String value, String remark) {
        SysConfig existing = sysConfigMapper.checkConfigKeyUnique(key);
        if (existing == null) {
            SysConfig config = new SysConfig();
            config.setConfigName(name);
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setConfigType("N");
            config.setCreateBy("admin");
            config.setRemark(remark);
            sysConfigMapper.insertConfig(config);
            return;
        }
        existing.setConfigName(name);
        existing.setConfigValue(value);
        existing.setConfigType("N");
        existing.setUpdateBy("admin");
        existing.setRemark(remark);
        sysConfigMapper.updateConfig(existing);
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

    private JsonNode readData(MvcResult result) throws Exception {
        return readBody(result).path("data");
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(content);
    }

    private static class ConfigSnapshot {
        private final Long configId;
        private final String configName;
        private final String configKey;
        private final String configValue;
        private final String configType;
        private final String remark;
        private Long insertedConfigId;

        private ConfigSnapshot(SysConfig config) {
            if (config == null) {
                this.configId = null;
                this.configName = null;
                this.configKey = null;
                this.configValue = null;
                this.configType = null;
                this.remark = null;
                return;
            }
            this.configId = config.getConfigId();
            this.configName = config.getConfigName();
            this.configKey = config.getConfigKey();
            this.configValue = config.getConfigValue();
            this.configType = config.getConfigType();
            this.remark = config.getRemark();
        }

        private boolean exists() {
            return configId != null;
        }

        private Long getInsertedConfigId() {
            return insertedConfigId;
        }

        private void setInsertedConfigId(Long insertedConfigId) {
            this.insertedConfigId = insertedConfigId;
        }

        private SysConfig toConfig() {
            SysConfig config = new SysConfig();
            config.setConfigId(configId);
            config.setConfigName(configName);
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setConfigType(configType);
            config.setRemark(remark);
            return config;
        }
    }
}
