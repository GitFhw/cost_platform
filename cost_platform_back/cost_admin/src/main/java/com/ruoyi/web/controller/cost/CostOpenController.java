package com.ruoyi.web.controller.cost;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.cost.CostScene;
import com.ruoyi.system.domain.cost.bo.CostFeeCalculateBo;
import com.ruoyi.system.domain.cost.bo.CostOpenTokenApplyBo;
import com.ruoyi.system.domain.cost.vo.CostOpenAppSession;
import com.ruoyi.system.service.cost.ICostOpenAppService;
import com.ruoyi.system.service.cost.ICostOpenTokenService;
import com.ruoyi.system.service.cost.ICostRunService;
import com.ruoyi.system.service.cost.ICostSceneService;
import com.ruoyi.web.interceptor.cost.CostOpenApiConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.ruoyi.system.service.cost.constant.CostDomainConstants.STATUS_ENABLED;

/**
 * 第三方开放接口控制器
 */
@RestController
@RequestMapping("/cost/open")
public class CostOpenController extends BaseController {
    private static final String SNAPSHOT_MODE_ACTIVE = "ACTIVE";
    private static final String SNAPSHOT_MODE_DRAFT = "DRAFT";

    @Autowired
    private ICostSceneService sceneService;

    @Autowired
    private ICostRunService runService;

    @Autowired
    private ICostOpenAppService openAppService;

    @Autowired
    private ICostOpenTokenService openTokenService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 第三方应用换取访问令牌。
     */
    @Log(title = "开放接口令牌", businessType = BusinessType.OTHER)
    @PostMapping("/auth/token")
    public AjaxResult issueToken(@Validated @RequestBody CostOpenTokenApplyBo bo) {
        return AjaxResult.success("开放接口访问令牌申请成功", openTokenService.issueToken(bo));
    }

    /**
     * 查询当前开放应用可访问的场景列表。
     */
    @GetMapping("/scenes")
    public AjaxResult sceneOptions(HttpServletRequest request) {
        CostOpenAppSession session = requireOpenSession(request);
        CostScene query = new CostScene();
        query.setStatus(STATUS_ENABLED);
        List<CostScene> scenes = sceneService.selectSceneOptions(query);
        List<Map<String, Object>> sceneItems = scenes.stream()
                .filter(scene -> openAppService.canAccessScene(session, scene.getSceneId()))
                .map(this::buildSceneItem)
                .toList();
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("openApp", buildOpenAppItem(session));
        result.put("sceneCount", sceneItems.size());
        result.put("scenes", sceneItems);
        return success(result);
    }

    /**
     * 查询场景可用版本口径。
     */
    @GetMapping("/scenes/{sceneId}/versions")
    public AjaxResult versionOptions(HttpServletRequest request, @PathVariable Long sceneId) {
        CostOpenAppSession session = requireOpenSession(request);
        CostScene scene = requireAuthorizedScene(session, sceneId);
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("openApp", buildOpenAppItem(session));
        result.put("scene", buildSceneItem(scene));
        result.put("defaultSnapshotMode", resolveDefaultSnapshotMode(scene, session));
        result.put("supportedSnapshotModes", buildSupportedSnapshotModes(session));
        result.put("publishedVersions", runService.selectVersionOptions(sceneId));
        return success(result);
    }

    /**
     * 查询场景在指定运行快照下的可执行费用。
     */
    @GetMapping("/scenes/{sceneId}/fees")
    public AjaxResult runtimeFeeOptions(HttpServletRequest request,
                                        @PathVariable Long sceneId,
                                        @RequestParam(value = "versionId", required = false) Long versionId,
                                        @RequestParam(value = "snapshotMode", required = false) String snapshotMode) {
        CostOpenAppSession session = requireOpenSession(request);
        CostScene scene = requireAuthorizedScene(session, sceneId);
        String normalizedSnapshotMode = normalizeSnapshotMode(snapshotMode);
        assertSnapshotModeAllowed(session, normalizedSnapshotMode);
        List<Map<String, Object>> feeItems = runService.selectRuntimeFeeOptions(sceneId, versionId, normalizedSnapshotMode);
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("openApp", buildOpenAppItem(session));
        result.put("scene", buildSceneItem(scene));
        result.put("requestedVersionId", versionId);
        result.put("snapshotMode", normalizedSnapshotMode);
        result.put("feeCount", feeItems.size());
        result.put("fees", feeItems);
        return success(result);
    }

    /**
     * 生成单费用、多费用或全费用的接入模板。
     */
    @GetMapping("/fee-template")
    public AjaxResult feeTemplate(HttpServletRequest request,
                                  @RequestParam("sceneId") Long sceneId,
                                  @RequestParam(value = "versionId", required = false) Long versionId,
                                  @RequestParam(value = "snapshotMode", required = false) String snapshotMode,
                                  @RequestParam(value = "feeIds", required = false) String feeIds,
                                  @RequestParam(value = "feeId", required = false) Long feeId,
                                  @RequestParam(value = "feeCode", required = false) String feeCode,
                                  @RequestParam(value = "taskType", required = false) String taskType) {
        CostOpenAppSession session = requireOpenSession(request);
        requireAuthorizedScene(session, sceneId);
        String normalizedSnapshotMode = normalizeSnapshotMode(snapshotMode);
        assertSnapshotModeAllowed(session, normalizedSnapshotMode);
        Map<String, Object> template = runService.buildFeeInputTemplate(sceneId, versionId, parseLongIdList(feeIds),
                feeId, feeCode, taskType, normalizedSnapshotMode);
        return success(buildOpenTemplateResult(template, normalizedSnapshotMode, session));
    }

    /**
     * 按指定费用范围执行开放接口取价。
     */
    @Log(title = "开放接口费用核算", businessType = BusinessType.INSERT)
    @PostMapping("/fee/calculate")
    public AjaxResult calculateFee(HttpServletRequest request, @Valid @RequestBody CostFeeCalculateBo bo) {
        CostOpenAppSession session = requireOpenSession(request);
        requireAuthorizedScene(session, bo.getSceneId());
        String normalizedSnapshotMode = normalizeSnapshotMode(bo.getSnapshotMode());
        assertSnapshotModeAllowed(session, normalizedSnapshotMode);
        bo.setSnapshotMode(normalizedSnapshotMode);
        AjaxResult validationError = validateOpenCalculationInput(bo, session);
        if (validationError != null) {
            return validationError;
        }
        Map<String, Object> result = runService.calculateFee(bo);
        result.put("validationPassed", true);
        result.put("snapshotMode", normalizedSnapshotMode);
        result.put("openApp", buildOpenAppItem(session));
        return success(result);
    }

    private CostScene requireAuthorizedScene(CostOpenAppSession session, Long sceneId) {
        openAppService.assertCanAccessScene(session, sceneId);
        CostScene scene = sceneService.selectSceneById(sceneId);
        if (scene == null) {
            throw new ServiceException("目标场景不存在，请刷新后重试");
        }
        return scene;
    }

    private CostOpenAppSession requireOpenSession(HttpServletRequest request) {
        Object session = request.getAttribute(CostOpenApiConstants.OPEN_APP_SESSION_ATTR);
        if (session instanceof CostOpenAppSession openSession) {
            return openSession;
        }
        throw new ServiceException("开放接口访问令牌无效，请重新申请 accessToken", HttpStatus.UNAUTHORIZED);
    }

    private void assertSnapshotModeAllowed(CostOpenAppSession session, String snapshotMode) {
        if (SNAPSHOT_MODE_DRAFT.equals(snapshotMode) && !openAppService.allowDraftSnapshot(session)) {
            throw new ServiceException("当前开放应用未开通草稿联调权限，请切换为生效版本或联系平台管理员开通", HttpStatus.FORBIDDEN);
        }
    }

    private String resolveDefaultSnapshotMode(CostScene scene, CostOpenAppSession session) {
        if (scene.getActiveVersionId() == null && openAppService.allowDraftSnapshot(session)) {
            return SNAPSHOT_MODE_DRAFT;
        }
        return SNAPSHOT_MODE_ACTIVE;
    }

    private List<Map<String, Object>> buildSupportedSnapshotModes(CostOpenAppSession session) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(buildSnapshotModeItem(SNAPSHOT_MODE_ACTIVE, "生效版本", "未指定 versionId 时，按场景当前生效版本执行。"));
        if (openAppService.allowDraftSnapshot(session)) {
            items.add(buildSnapshotModeItem(SNAPSHOT_MODE_DRAFT, "草稿配置", "允许在未发布配置上联调模板和单费用取价，适合第三方对接前置验证。"));
        }
        return items;
    }

    private Map<String, Object> buildSnapshotModeItem(String code, String label, String desc) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("label", label);
        item.put("description", desc);
        return item;
    }

    private Map<String, Object> buildOpenAppItem(CostOpenAppSession session) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        item.put("appCode", session.getAppCode());
        item.put("appName", session.getAppName());
        item.put("sceneScopeType", session.getSceneScopeType());
        item.put("draftSnapshotAllowed", session.getAllowDraftSnapshot());
        item.put("authorizedSceneIds", session.getAuthorizedSceneIds());
        return item;
    }

    private Map<String, Object> buildSceneItem(CostScene scene) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        item.put("sceneId", scene.getSceneId());
        item.put("sceneCode", scene.getSceneCode());
        item.put("sceneName", scene.getSceneName());
        item.put("businessDomain", scene.getBusinessDomain());
        item.put("defaultObjectDimension", scene.getDefaultObjectDimension());
        item.put("activeVersionId", scene.getActiveVersionId());
        item.put("activeVersionNo", scene.getActiveVersionNo());
        item.put("status", scene.getStatus());
        return item;
    }

    private Map<String, Object> buildOpenTemplateResult(Map<String, Object> template,
                                                        String snapshotMode,
                                                        CostOpenAppSession session) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>(template);
        List<Map<String, Object>> inputContractFields = buildOpenTemplateFields(template.get("fields"));
        result.put("openApp", buildOpenAppItem(session));
        result.put("snapshotMode", normalizeSnapshotMode(snapshotMode));
        result.put("inputContractFieldCount", inputContractFields.size());
        result.put("requiredFieldCount", inputContractFields.stream()
                .filter(item -> Boolean.TRUE.equals(item.get("required")))
                .count());
        result.put("inputContractFields", inputContractFields);
        result.put("integrationAdvice", buildTemplateAdvice(result, session));
        return result;
    }

    private List<Map<String, Object>> buildOpenTemplateFields(Object fieldsObject) {
        if (!(fieldsObject instanceof List<?> fields)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : fields) {
            if (!(item instanceof Map<?, ?> rawField)) {
                continue;
            }
            String path = stringValue(rawField.get("path"));
            Object defaultValue = rawField.get("defaultValue");
            boolean includedInTemplate = booleanValue(rawField.get("includedInTemplate"));
            boolean required = includedInTemplate && !hasUsableDefaultValue(defaultValue);
            LinkedHashMap<String, Object> field = new LinkedHashMap<>();
            field.put("variableCode", stringValue(rawField.get("variableCode")));
            field.put("variableName", stringValue(rawField.get("variableName")));
            field.put("sourceType", stringValue(rawField.get("sourceType")));
            field.put("dataType", stringValue(rawField.get("dataType")));
            field.put("sourceSystem", stringValue(rawField.get("sourceSystem")));
            field.put("path", path);
            field.put("pathLabel", StringUtils.isNotEmpty(path) ? path : stringValue(rawField.get("variableCode")));
            field.put("required", required);
            field.put("defaultValue", defaultValue);
            field.put("exampleValue", rawField.get("exampleValue"));
            field.put("includedInTemplate", includedInTemplate);
            field.put("templateRoles", rawField.get("templateRoles"));
            field.put("sourceRuleCodes", rawField.get("sourceRuleCodes"));
            field.put("dependsOn", rawField.get("dependsOn"));
            result.add(field);
        }
        return result;
    }

    private List<String> buildTemplateAdvice(Map<String, Object> template, CostOpenAppSession session) {
        List<String> advice = new ArrayList<>();
        if (Objects.equals(template.get("snapshotSource"), "DRAFT")) {
            advice.add("当前模板来自草稿配置，适合第三方在未发布口径上做联调验证。");
        } else {
            advice.add("当前模板来自已发布运行快照，适合第三方按稳定口径对接和压测。");
        }
        if (Boolean.TRUE.equals(template.get("allFeeScope"))) {
            advice.add("当前模板按全费用执行链拉平了所需字段，适合做整场景联调、全费用压测或批量导入。");
        } else {
            advice.add("当前模板已按目标费用及其依赖费用收敛，只需准备相关字段即可。");
        }
        if (!openAppService.allowDraftSnapshot(session)) {
            advice.add("当前开放应用未开通草稿联调权限，正式接入时只能使用已发布生效口径。");
        }
        return advice;
    }

    private AjaxResult validateOpenCalculationInput(CostFeeCalculateBo bo, CostOpenAppSession session) {
        Map<String, Object> template = runService.buildFeeInputTemplate(bo.getSceneId(), bo.getVersionId(), bo.getFeeIds(),
                bo.getFeeId(), bo.getFeeCode(), null, bo.getSnapshotMode());
        List<Map<String, Object>> fields = buildOpenTemplateFields(template.get("fields"));
        List<Map<String, Object>> requiredFields = fields.stream()
                .filter(item -> Boolean.TRUE.equals(item.get("required")))
                .filter(item -> StringUtils.isNotEmpty(stringValue(item.get("path"))))
                .toList();
        if (requiredFields.isEmpty()) {
            return null;
        }
        List<JsonNode> inputs = parseInputNodes(bo.getInputJson());
        List<Map<String, Object>> validationMessages = new ArrayList<>();
        for (int index = 0; index < inputs.size(); index++) {
            JsonNode item = inputs.get(index);
            String bizNo = firstNonBlank(textValue(item.get("bizNo")), textValue(item.get("biz_no")),
                    "record-" + (index + 1));
            for (Map<String, Object> field : requiredFields) {
                String path = stringValue(field.get("path"));
                JsonNode valueNode = resolvePath(item, path);
                if (isMissingValue(valueNode)) {
                    LinkedHashMap<String, Object> message = new LinkedHashMap<>();
                    message.put("recordIndex", index + 1);
                    message.put("bizNo", bizNo);
                    message.put("variableCode", field.get("variableCode"));
                    message.put("variableName", field.get("variableName"));
                    message.put("path", path);
                    message.put("message", "缺少必填字段 " + field.get("variableName") + "，请按来源路径补齐：" + path);
                    validationMessages.add(message);
                }
            }
        }
        if (validationMessages.isEmpty()) {
            return null;
        }
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("openApp", buildOpenAppItem(session));
        data.put("sceneId", template.get("sceneId"));
        data.put("sceneCode", template.get("sceneCode"));
        data.put("sceneName", template.get("sceneName"));
        data.put("versionId", template.get("versionId"));
        data.put("versionNo", template.get("versionNo"));
        data.put("snapshotSource", template.get("snapshotSource"));
        data.put("snapshotMode", normalizeSnapshotMode(bo.getSnapshotMode()));
        data.put("fee", template.get("fee"));
        data.put("validationPassed", false);
        data.put("missingFieldCount", validationMessages.size());
        data.put("validationMessages", validationMessages);
        return AjaxResult.error("输入数据缺少模板要求字段，请补齐后再重新取价", data);
    }

    private List<JsonNode> parseInputNodes(String inputJson) {
        try {
            JsonNode root = objectMapper.readTree(inputJson);
            if (root == null || root.isNull()) {
                throw new ServiceException("费用核算输入不能为空");
            }
            if (root.isObject()) {
                return Collections.singletonList(root);
            }
            if (root.isArray()) {
                List<JsonNode> items = new ArrayList<>();
                root.forEach(items::add);
                if (items.isEmpty()) {
                    throw new ServiceException("费用核算输入不能为空数组");
                }
                return items;
            }
            throw new ServiceException("费用核算输入必须是 JSON 对象或对象数组");
        } catch (IOException e) {
            throw new ServiceException("费用核算输入不是合法 JSON，请检查后重试");
        }
    }

    private JsonNode resolvePath(JsonNode root, String path) {
        JsonNode current = root;
        for (String segment : StringUtils.split(path, ".")) {
            if (current == null || current.isMissingNode() || current.isNull()) {
                return null;
            }
            current = current.get(segment);
        }
        return current;
    }

    private boolean isMissingValue(JsonNode valueNode) {
        if (valueNode == null || valueNode.isNull() || valueNode.isMissingNode()) {
            return true;
        }
        return valueNode.isTextual() && StringUtils.isEmpty(valueNode.asText());
    }

    private boolean hasUsableDefaultValue(Object defaultValue) {
        if (defaultValue == null) {
            return false;
        }
        return !(defaultValue instanceof String stringValue) || StringUtils.isNotEmpty(stringValue);
    }

    private String normalizeSnapshotMode(String snapshotMode) {
        if (StringUtils.isBlank(snapshotMode)) {
            return SNAPSHOT_MODE_ACTIVE;
        }
        return SNAPSHOT_MODE_DRAFT.equalsIgnoreCase(snapshotMode.trim())
                ? SNAPSHOT_MODE_DRAFT : SNAPSHOT_MODE_ACTIVE;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String textValue(JsonNode node) {
        return node == null || node.isNull() ? "" : node.asText();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotEmpty(value)) {
                return value;
            }
        }
        return "";
    }

    private List<Long> parseLongIdList(String ids) {
        if (StringUtils.isBlank(ids)) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        for (String item : ids.split(",")) {
            String value = StringUtils.trim(item);
            if (StringUtils.isBlank(value)) {
                continue;
            }
            try {
                result.add(Long.valueOf(value));
            } catch (NumberFormatException ex) {
                throw new ServiceException("feeIds 格式不正确，请传逗号分隔的数字主键列表");
            }
        }
        return result;
    }
}
