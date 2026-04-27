package com.ruoyi.web.controller.cost;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.cost.CostScene;
import com.ruoyi.system.domain.cost.bo.CostFeeCalculateBo;
import com.ruoyi.system.service.cost.ICostRunService;
import com.ruoyi.system.service.cost.ICostSceneService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

import static com.ruoyi.system.service.cost.constant.CostDomainConstants.STATUS_ENABLED;

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
    private ObjectMapper objectMapper;

    @PreAuthorize("@ss.hasPermi('cost:access:list') or @ss.hasPermi('cost:scene:list') or @ss.hasPermi('cost:simulation:list')")
    @GetMapping("/scenes")
    public AjaxResult sceneOptions() {
        CostScene query = new CostScene();
        query.setStatus(STATUS_ENABLED);
        List<CostScene> scenes = sceneService.selectSceneOptions(query);
        List<Map<String, Object>> sceneItems = scenes.stream()
                .map(this::buildSceneItem)
                .toList();
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("sceneCount", sceneItems.size());
        result.put("scenes", sceneItems);
        return success(result);
    }

    @PreAuthorize("@ss.hasPermi('cost:access:list') or @ss.hasPermi('cost:simulation:list') or @ss.hasPermi('cost:task:list')")
    @GetMapping("/scenes/{sceneId}/versions")
    public AjaxResult versionOptions(@PathVariable Long sceneId) {
        CostScene scene = requireScene(sceneId);
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("scene", buildSceneItem(scene));
        result.put("defaultSnapshotMode", scene.getActiveVersionId() == null ? SNAPSHOT_MODE_DRAFT : SNAPSHOT_MODE_ACTIVE);
        result.put("supportedSnapshotModes", buildSupportedSnapshotModes());
        result.put("publishedVersions", runService.selectVersionOptions(sceneId));
        return success(result);
    }

    @PreAuthorize("@ss.hasPermi('cost:access:list') or @ss.hasPermi('cost:simulation:list') or @ss.hasPermi('cost:task:list')")
    @GetMapping("/scenes/{sceneId}/fees")
    public AjaxResult runtimeFeeOptions(@PathVariable Long sceneId,
                                        @RequestParam(value = "versionId", required = false) Long versionId,
                                        @RequestParam(value = "snapshotMode", required = false) String snapshotMode) {
        CostScene scene = requireScene(sceneId);
        List<Map<String, Object>> feeItems = runService.selectRuntimeFeeOptions(sceneId, versionId, snapshotMode);
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("scene", buildSceneItem(scene));
        result.put("requestedVersionId", versionId);
        result.put("snapshotMode", normalizeSnapshotMode(snapshotMode));
        result.put("feeCount", feeItems.size());
        result.put("fees", feeItems);
        return success(result);
    }

    @PreAuthorize("@ss.hasPermi('cost:access:list') or @ss.hasPermi('cost:simulation:list') or @ss.hasPermi('cost:task:list')")
    @GetMapping("/fee-template")
    public AjaxResult feeTemplate(@RequestParam("sceneId") Long sceneId,
                                  @RequestParam(value = "versionId", required = false) Long versionId,
                                  @RequestParam(value = "snapshotMode", required = false) String snapshotMode,
                                  @RequestParam(value = "feeIds", required = false) String feeIds,
                                  @RequestParam(value = "feeId", required = false) Long feeId,
                                  @RequestParam(value = "feeCode", required = false) String feeCode,
                                  @RequestParam(value = "taskType", required = false) String taskType) {
        requireScene(sceneId);
        Map<String, Object> template = runService.buildFeeInputTemplate(sceneId, versionId, parseLongIdList(feeIds),
                feeId, feeCode, taskType, snapshotMode);
        return success(buildOpenTemplateResult(template, snapshotMode));
    }

    @PreAuthorize("@ss.hasPermi('cost:simulation:execute') or @ss.hasPermi('cost:task:execute')")
    @Log(title = "第三方开放核算", businessType = BusinessType.INSERT)
    @PostMapping("/fee/calculate")
    public AjaxResult calculateFee(@Valid @RequestBody CostFeeCalculateBo bo) {
        requireScene(bo.getSceneId());
        AjaxResult validationError = validateOpenCalculationInput(bo);
        if (validationError != null) {
            return validationError;
        }
        Map<String, Object> result = runService.calculateFee(bo);
        result.put("validationPassed", true);
        result.put("snapshotMode", normalizeSnapshotMode(bo.getSnapshotMode()));
        return success(result);
    }

    private CostScene requireScene(Long sceneId) {
        CostScene scene = sceneService.selectSceneById(sceneId);
        if (scene == null) {
            throw new ServiceException("目标场景不存在，请刷新后重试");
        }
        return scene;
    }

    private List<Map<String, Object>> buildSupportedSnapshotModes() {
        return List.of(
                buildSnapshotModeItem(SNAPSHOT_MODE_ACTIVE, "生效版本", "未指定 versionId 时，按场景当前生效版本执行。"),
                buildSnapshotModeItem(SNAPSHOT_MODE_DRAFT, "草稿配置", "未指定 versionId 时，按场景当前草稿配置执行，适合联调未发布口径。")
        );
    }

    private Map<String, Object> buildSnapshotModeItem(String code, String label, String desc) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("label", label);
        item.put("description", desc);
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

    private Map<String, Object> buildOpenTemplateResult(Map<String, Object> template, String snapshotMode) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>(template);
        List<Map<String, Object>> inputContractFields = buildOpenTemplateFields(template.get("fields"));
        result.put("snapshotMode", normalizeSnapshotMode(snapshotMode));
        result.put("inputContractFieldCount", inputContractFields.size());
        result.put("requiredFieldCount", inputContractFields.stream()
                .filter(item -> Boolean.TRUE.equals(item.get("required")))
                .count());
        result.put("inputContractFields", inputContractFields);
        result.put("integrationAdvice", buildTemplateAdvice(result));
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

    private List<String> buildTemplateAdvice(Map<String, Object> template) {
        List<String> advice = new ArrayList<>();
        if (Objects.equals(template.get("snapshotSource"), "DRAFT")) {
            advice.add("当前模板来自草稿配置，适合联调未发布规则；正式接入上线前建议再按生效版本回归一次。");
        } else {
            advice.add("当前模板来自已发布运行快照，适合第三方按稳定口径联调与上线。");
        }
        if (Boolean.TRUE.equals(template.get("allFeeScope"))) {
            advice.add("当前请求未指定目标费用，模板会拉平当前场景全部费用执行链需要的字段。");
        } else {
            advice.add("当前模板已按目标费用及其依赖费用执行链收敛，只需要准备相关字段。");
        }
        return advice;
    }

    private AjaxResult validateOpenCalculationInput(CostFeeCalculateBo bo) {
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
                    message.put("message", "缺少必需字段 " + field.get("variableName") + "，请按来源路径补齐：" + path);
                    validationMessages.add(message);
                }
            }
        }
        if (validationMessages.isEmpty()) {
            return null;
        }
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
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
        return AjaxResult.error("输入数据缺少模板要求字段，请先补齐后再核算", data);
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
        if (StringUtils.isEmpty(snapshotMode)) {
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
        if (StringUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        for (String item : ids.split(",")) {
            String value = StringUtils.trim(item);
            if (StringUtils.isNotEmpty(value)) {
                result.add(Long.valueOf(value));
            }
        }
        return result;
    }
}
