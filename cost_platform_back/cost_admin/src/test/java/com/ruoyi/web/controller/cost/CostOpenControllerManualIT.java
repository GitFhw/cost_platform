package com.ruoyi.web.controller.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.cost.CostFeeItem;
import com.ruoyi.system.domain.cost.CostPublishVersion;
import com.ruoyi.system.domain.cost.CostScene;
import com.ruoyi.system.mapper.cost.CostFeeMapper;
import com.ruoyi.system.mapper.cost.CostPublishVersionMapper;
import com.ruoyi.system.mapper.cost.CostSceneMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CostOpenControllerManualIT {
    private static final DateTimeFormatter STAMP_FORMATTER = DateTimeFormatter.ofPattern("HHmmssSSS");
    private static final String SCENE_CODE = "SHOUGANG-ORE-HR-001";
    private static final String OPEN_APP_CODE = "DEMO_OPEN_APP";
    private static final String OPEN_APP_SECRET = "demo-open-secret";
    private static final String PUBLISHED_APP_CODE = "DEMO_PUBLISHED_APP";
    private static final String PUBLISHED_APP_SECRET = "demo-published-secret";
    private static final String FEMALE_FEE_CODE = "SG_FEMALE_SHIFT_LABOR";
    private static final String COVER_FEE_CODE = "SG_COVER_ODD_JOB_LABOR";

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
    private CostFeeMapper feeMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepareOpenApps() {
        jdbcTemplate.execute("""
                create table if not exists cost_open_app (
                    app_id bigint primary key auto_increment comment '开放应用主键',
                    app_code varchar(64) not null comment '开放应用编码',
                    app_name varchar(128) not null comment '开放应用名称',
                    app_secret_hash char(64) not null comment '开放应用密钥 SHA-256 摘要',
                    scene_scope_type varchar(16) not null default 'ALL' comment '场景授权范围类型',
                    scene_ids_json longtext null comment '授权场景主键 JSON',
                    allow_draft_snapshot tinyint(1) not null default 0 comment '是否允许草稿联调',
                    token_ttl_seconds int not null default 7200 comment '访问令牌有效期（秒）',
                    effective_start_time datetime null comment '生效开始时间',
                    effective_end_time datetime null comment '生效结束时间',
                    status char(1) not null default '0' comment '状态（0正常 1停用）',
                    sort_no int null default 0 comment '排序号',
                    create_by varchar(64) null comment '创建人',
                    create_time datetime null comment '创建时间',
                    update_by varchar(64) null comment '更新人',
                    update_time datetime null comment '更新时间',
                    remark varchar(500) null comment '备注',
                    unique key uk_cost_open_app_code (app_code),
                    key idx_cost_open_app_status (status)
                ) engine=innodb
                """);
        jdbcTemplate.update("delete from cost_open_app where app_code in (?, ?)", OPEN_APP_CODE, PUBLISHED_APP_CODE);
        jdbcTemplate.update("""
                        insert into cost_open_app (
                            app_code, app_name, app_secret_hash, scene_scope_type, scene_ids_json,
                            allow_draft_snapshot, token_ttl_seconds, status, sort_no, create_by, create_time, remark
                        ) values (?, ?, ?, 'ALL', null, ?, 7200, '0', ?, 'admin', now(), ?)
                        """,
                OPEN_APP_CODE, "开放联调演示应用",
                "2184162593f8f03e1334c93d92419bac0ca371e967a2bd77274eb172598976d0",
                1, 10, "测试用：允许使用草稿联调");
        jdbcTemplate.update("""
                        insert into cost_open_app (
                            app_code, app_name, app_secret_hash, scene_scope_type, scene_ids_json,
                            allow_draft_snapshot, token_ttl_seconds, status, sort_no, create_by, create_time, remark
                        ) values (?, ?, ?, 'ALL', null, ?, 7200, '0', ?, 'admin', now(), ?)
                        """,
                PUBLISHED_APP_CODE, "开放生产演示应用",
                "3b3f916eede6dfc6f56b24b9399757406bfc7d989062eb4ff4930a3cb9caac5f",
                0, 20, "测试用：仅允许使用已发布生效版本");
    }

    @Test
    void shouldIssueOpenTokenForAuthorizedApp() throws Exception {
        JsonNode result = readData(mockMvc.perform(post("/cost/open/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "appCode", OPEN_APP_CODE,
                                "appSecret", OPEN_APP_SECRET))))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(result.path("appCode").asText()).isEqualTo(OPEN_APP_CODE);
        assertThat(result.path("tokenType").asText()).isEqualTo("Bearer");
        assertThat(result.path("accessToken").asText()).startsWith("coa_");
        assertThat(result.path("draftSnapshotAllowed").asBoolean()).isTrue();
    }

    @Test
    void shouldExposeOpenFeeTemplateForDraftSnapshot() throws Exception {
        Long sceneId = requireSceneId();
        String accessToken = issueOpenToken(OPEN_APP_CODE, OPEN_APP_SECRET);

        JsonNode template = readData(mockMvc.perform(authorized(get("/cost/open/fee-template"), accessToken)
                        .param("sceneId", String.valueOf(sceneId))
                        .param("snapshotMode", "DRAFT")
                        .param("feeCode", FEMALE_FEE_CODE))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(template.path("snapshotSource").asText()).isEqualTo("DRAFT");
        assertThat(template.path("snapshotMode").asText()).isEqualTo("DRAFT");
        assertThat(template.path("inputContractFieldCount").asInt()).isGreaterThanOrEqualTo(3);
        assertThat(findNodeByField(template.path("inputContractFields"), "variableCode", "FEMALE_TEAM_HEADCOUNT")).isNotNull();
        assertThat(findNodeByField(template.path("inputContractFields"), "variableCode", "FEMALE_ACTUAL_ATTENDANCE")).isNotNull();
        assertThat(findNodeByField(template.path("inputContractFields"), "variableCode", "FEMALE_REQUIRED_ATTENDANCE")).isNotNull();
    }

    @Test
    void shouldReturnFriendlyValidationMessagesForOpenFeeCalculate() throws Exception {
        Long sceneId = requireSceneId();
        String accessToken = issueOpenToken(OPEN_APP_CODE, OPEN_APP_SECRET);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("sceneId", sceneId);
        body.put("snapshotMode", "DRAFT");
        body.put("feeCode", COVER_FEE_CODE);
        body.put("inputJson", objectMapper.writeValueAsString(createIncompleteCoverInput()));

        JsonNode result = readBody(mockMvc.perform(authorized(post("/cost/open/fee/calculate"), accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(result.path("code").asInt()).isEqualTo(500);
        assertThat(result.path("data").path("validationPassed").asBoolean()).isFalse();
        assertThat(result.path("data").path("missingFieldCount").asInt()).isGreaterThanOrEqualTo(2);
        assertThat(findNodeByField(result.path("data").path("validationMessages"), "variableCode", "COVER_ACTION")).isNotNull();
        assertThat(findNodeByField(result.path("data").path("validationMessages"), "variableCode", "COVER_CARGO_TYPE")).isNotNull();
    }

    @Test
    void shouldCalculateMultipleOpenFeesAgainstPublishedVersion() throws Exception {
        Long sceneId = requireSceneId();
        String adminAuthorization = "Bearer " + loginAndGetToken();
        String accessToken = issueOpenToken(OPEN_APP_CODE, OPEN_APP_SECRET);
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, adminAuthorization, "open-api-published-" + stamp);
        List<Long> feeIds = requireFeeIds(sceneId, FEMALE_FEE_CODE, COVER_FEE_CODE);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("sceneId", sceneId);
        body.put("versionId", versionId);
        body.put("includeExplain", false);
        body.set("feeIds", objectMapper.valueToTree(feeIds));
        body.put("inputJson", objectMapper.writeValueAsString(createCombinedInput()));

        JsonNode result = readData(mockMvc.perform(authorized(post("/cost/open/fee/calculate"), accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(result.path("snapshotSource").asText()).isEqualTo("PUBLISHED");
        assertThat(result.path("targetFeeCount").asInt()).isEqualTo(2);
        assertThat(result.path("successCount").asInt()).isEqualTo(2);
        assertThat(result.path("records").isArray()).isTrue();
        assertThat(result.path("records").size()).isEqualTo(2);
    }

    @Test
    void shouldRejectDraftSnapshotWhenAppHasNoDraftPermission() throws Exception {
        Long sceneId = requireSceneId();
        String accessToken = issueOpenToken(PUBLISHED_APP_CODE, PUBLISHED_APP_SECRET);

        JsonNode result = readBody(mockMvc.perform(authorized(get("/cost/open/fee-template"), accessToken)
                        .param("sceneId", String.valueOf(sceneId))
                        .param("snapshotMode", "DRAFT")
                        .param("feeCode", FEMALE_FEE_CODE))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(result.path("code").asInt()).isEqualTo(403);
        assertThat(result.path("msg").asText()).contains("草稿联调权限");
    }

    @Test
    void shouldExposePublishedVersionsAndRuntimeFeesForOpenApi() throws Exception {
        Long sceneId = requireSceneId();
        String adminAuthorization = "Bearer " + loginAndGetToken();
        String accessToken = issueOpenToken(OPEN_APP_CODE, OPEN_APP_SECRET);
        String stamp = LocalTime.now().format(STAMP_FORMATTER);
        Long versionId = publishScene(sceneId, adminAuthorization, "open-api-version-list-" + stamp);

        JsonNode versions = readData(mockMvc.perform(authorized(get("/cost/open/scenes/{sceneId}/versions", sceneId), accessToken))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode fees = readData(mockMvc.perform(authorized(get("/cost/open/scenes/{sceneId}/fees", sceneId), accessToken)
                        .param("versionId", String.valueOf(versionId)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(findNodeByField(versions.path("publishedVersions"), "versionId", String.valueOf(versionId))).isNotNull();
        assertThat(versions.path("supportedSnapshotModes").isArray()).isTrue();
        assertThat(fees.path("feeCount").asInt()).isGreaterThan(0);
        assertThat(findNodeByField(fees.path("fees"), "feeCode", FEMALE_FEE_CODE)).isNotNull();
    }

    private Long requireSceneId() {
        CostScene scene = sceneMapper.selectOne(Wrappers.<CostScene>lambdaQuery()
                .eq(CostScene::getSceneCode, SCENE_CODE));
        assertThat(scene).isNotNull();
        return scene.getSceneId();
    }

    private Long publishScene(Long sceneId, String authorization, String publishDesc) throws Exception {
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

    private List<Long> requireFeeIds(Long sceneId, String... feeCodes) {
        Map<String, Long> feeIdMap = feeMapper.selectList(Wrappers.<CostFeeItem>lambdaQuery()
                        .eq(CostFeeItem::getSceneId, sceneId)
                        .in(CostFeeItem::getFeeCode, Arrays.asList(feeCodes)))
                .stream()
                .collect(Collectors.toMap(CostFeeItem::getFeeCode, CostFeeItem::getFeeId));
        List<Long> result = new ArrayList<>();
        for (String feeCode : feeCodes) {
            Long feeId = feeIdMap.get(feeCode);
            assertThat(feeId).isNotNull();
            result.add(feeId);
        }
        return result;
    }

    private ObjectNode createIncompleteCoverInput() {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("bizNo", "OPEN-VAL-002");
        item.put("objectCode", "OBJ-VAL-002");
        item.put("objectName", "开放校验对象");
        item.put("COVER_WORKLOAD_TON", 1000);
        return item;
    }

    private ObjectNode createCombinedInput() {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("bizNo", "OPEN-CALC-001");
        item.put("objectCode", "OBJ-CALC-001");
        item.put("objectName", "开放核算对象");
        item.put("FEMALE_TEAM_HEADCOUNT", 6);
        item.put("FEMALE_ACTUAL_ATTENDANCE", 6);
        item.put("FEMALE_REQUIRED_ATTENDANCE", 6);
        item.put("COVER_ACTION", "COVER");
        item.put("COVER_CARGO_TYPE", "COAL");
        item.put("COVER_WORKLOAD_TON", 1000);
        return item;
    }

    private String issueOpenToken(String appCode, String appSecret) throws Exception {
        JsonNode result = readData(mockMvc.perform(post("/cost/open/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "appCode", appCode,
                                "appSecret", appSecret))))
                .andExpect(status().isOk())
                .andReturn());
        return result.path("accessToken").asText();
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

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder builder, String accessToken) {
        return builder.header("Authorization", "Bearer " + accessToken);
    }

    private JsonNode findNodeByField(JsonNode items, String fieldName, String expectedValue) {
        if (items == null || !items.isArray()) {
            return null;
        }
        for (JsonNode item : items) {
            if (expectedValue.equals(item.path(fieldName).asText())) {
                return item;
            }
        }
        return null;
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return readBody(result).path("data");
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response);
    }
}
