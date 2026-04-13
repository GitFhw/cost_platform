package com.ruoyi.system.service.cost.publish;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.cost.publish.validation.PublishJsonSupport;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PublishSnapshotViewService {

    public PublishSnapshotBundle normalizeBundle(PublishSnapshotBundle bundle) {
        return bundle == null ? new PublishSnapshotBundle() : bundle;
    }

    public List<Map<String, Object>> buildFeeDiffSummary(PublishSnapshotBundle fromBundle,
                                                         PublishSnapshotBundle toBundle,
                                                         String feeCode) {
        fromBundle = normalizeBundle(fromBundle);
        toBundle = normalizeBundle(toBundle);
        Set<String> feeCodes = new TreeSet<>();
        feeCodes.addAll(fromBundle.feesByCode.keySet());
        feeCodes.addAll(toBundle.feesByCode.keySet());
        List<Map<String, Object>> result = new ArrayList<>();
        for (String code : feeCodes) {
            if (StringUtils.isNotEmpty(feeCode) && !StringUtils.equals(code, feeCode)) {
                continue;
            }
            Map<String, Object> fromFee = fromBundle.feesByCode.get(code);
            Map<String, Object> toFee = toBundle.feesByCode.get(code);
            String changeType = determineChangeType(fromFee, toFee);
            int ruleChangeCount = countFeeRuleChanges(fromBundle, toBundle, code);
            int variableChangeCount = countFeeVariableChanges(fromBundle, toBundle, code);
            boolean feeDirectChanged = !"UNCHANGED".equals(changeType);
            if (!feeDirectChanged && ruleChangeCount == 0 && variableChangeCount == 0) {
                continue;
            }
            LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            item.put("feeCode", code);
            item.put("feeName", firstNonBlank(
                    stringValue(firstNonNull(fromFee == null ? null : fromFee.get("feeName"), toFee == null ? null : toFee.get("feeName"))),
                    stringValue(firstNonNull(fromFee == null ? null : fromFee.get("feeCode"), toFee == null ? null : toFee.get("feeCode")))));
            item.put("changeType", changeType);
            item.put("changedFields", compareChangedFields(fromFee, toFee));
            item.put("ruleChangeCount", ruleChangeCount);
            item.put("variableChangeCount", variableChangeCount);
            item.put("summary", buildFeeSummaryText(changeType, feeDirectChanged, ruleChangeCount, variableChangeCount));
            item.put("fromFee", fromFee);
            item.put("toFee", toFee);
            item.put("fromRules", buildFeeRuleCompositeList(fromBundle, code));
            item.put("toRules", buildFeeRuleCompositeList(toBundle, code));
            item.put("fromVariables", buildFeeVariableList(fromBundle, code));
            item.put("toVariables", buildFeeVariableList(toBundle, code));
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> buildRuleDiffSummary(PublishSnapshotBundle fromBundle,
                                                          PublishSnapshotBundle toBundle,
                                                          String feeCode) {
        fromBundle = normalizeBundle(fromBundle);
        toBundle = normalizeBundle(toBundle);
        Set<String> ruleCodes = new TreeSet<>();
        if (StringUtils.isEmpty(feeCode)) {
            ruleCodes.addAll(fromBundle.rulesByCode.keySet());
            ruleCodes.addAll(toBundle.rulesByCode.keySet());
        } else {
            ruleCodes.addAll(fromBundle.feeRuleCodes.getOrDefault(feeCode, new TreeSet<>()));
            ruleCodes.addAll(toBundle.feeRuleCodes.getOrDefault(feeCode, new TreeSet<>()));
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String ruleCode : ruleCodes) {
            Map<String, Object> fromRule = fromBundle.rulesByCode.get(ruleCode);
            Map<String, Object> toRule = toBundle.rulesByCode.get(ruleCode);
            String changeType = determineChangeType(fromRule, toRule);
            int conditionChangeCount = countCollectionDiff(
                    fromBundle == null ? null : fromBundle.ruleConditionsByRuleCode.get(ruleCode),
                    toBundle == null ? null : toBundle.ruleConditionsByRuleCode.get(ruleCode));
            int tierChangeCount = countCollectionDiff(
                    fromBundle == null ? null : fromBundle.ruleTiersByRuleCode.get(ruleCode),
                    toBundle == null ? null : toBundle.ruleTiersByRuleCode.get(ruleCode));
            if ("UNCHANGED".equals(changeType) && conditionChangeCount == 0 && tierChangeCount == 0) {
                continue;
            }
            LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            item.put("ruleCode", ruleCode);
            item.put("ruleName", firstNonBlank(
                    stringValue(firstNonNull(fromRule == null ? null : fromRule.get("ruleName"), toRule == null ? null : toRule.get("ruleName"))),
                    stringValue(firstNonNull(fromRule == null ? null : fromRule.get("ruleCode"), toRule == null ? null : toRule.get("ruleCode")))));
            item.put("feeCode", firstNonBlank(
                    stringValue(firstNonNull(fromRule == null ? null : fromRule.get("feeCode"), toRule == null ? null : toRule.get("feeCode"))),
                    stringValue(firstNonNull(fromRule == null ? null : fromRule.get("ruleCode"), toRule == null ? null : toRule.get("ruleCode")))));
            item.put("changeType", changeType);
            item.put("changedFields", compareChangedFields(fromRule, toRule));
            item.put("conditionChangeCount", conditionChangeCount);
            item.put("tierChangeCount", tierChangeCount);
            item.put("fromRule", fromRule);
            item.put("toRule", toRule);
            item.put("fromConditions", normalizeCollection(fromBundle == null ? null : fromBundle.ruleConditionsByRuleCode.get(ruleCode)));
            item.put("toConditions", normalizeCollection(toBundle == null ? null : toBundle.ruleConditionsByRuleCode.get(ruleCode)));
            item.put("fromTiers", normalizeCollection(fromBundle == null ? null : fromBundle.ruleTiersByRuleCode.get(ruleCode)));
            item.put("toTiers", normalizeCollection(toBundle == null ? null : toBundle.ruleTiersByRuleCode.get(ruleCode)));
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> buildSceneDiffSummary(Map<String, Object> fromScene, Map<String, Object> toScene) {
        Set<String> fields = new LinkedHashSet<>();
        if (fromScene != null) fields.addAll(fromScene.keySet());
        if (toScene != null) fields.addAll(toScene.keySet());
        List<Map<String, Object>> result = new ArrayList<>();
        for (String field : fields) {
            Object fromValue = fromScene == null ? null : fromScene.get(field);
            Object toValue = toScene == null ? null : toScene.get(field);
            if (Objects.equals(PublishJsonSupport.canonicalJson(fromValue), PublishJsonSupport.canonicalJson(toValue))) {
                continue;
            }
            LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            item.put("field", field);
            item.put("fieldLabel", resolveSceneFieldLabel(field));
            item.put("fromValue", fromValue);
            item.put("toValue", toValue);
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> buildSnapshotCounts(PublishSnapshotBundle bundle, String feeCode) {
        bundle = normalizeBundle(bundle);
        LinkedHashMap<String, Object> counts = new LinkedHashMap<>();
        counts.put("scene", bundle.sceneSnapshot.isEmpty() ? 0 : 1);
        counts.put("fee", StringUtils.isEmpty(feeCode) ? bundle.feesByCode.size() : (bundle.feesByCode.containsKey(feeCode) ? 1 : 0));
        counts.put("variable", buildFeeVariableList(bundle, feeCode).size());
        counts.put("formula", bundle.formulasByCode.size());
        counts.put("rule", buildFeeRuleList(bundle, feeCode).size());
        counts.put("condition", buildFeeConditionList(bundle, feeCode).size());
        counts.put("tier", buildFeeTierList(bundle, feeCode).size());
        return counts;
    }

    public Map<String, Object> buildSnapshotGroups(PublishSnapshotBundle bundle, String feeCode) {
        bundle = normalizeBundle(bundle);
        LinkedHashMap<String, Object> groups = new LinkedHashMap<>();
        groups.put("scene", bundle.sceneSnapshot);
        groups.put("fees", buildFeeList(bundle, feeCode));
        groups.put("variables", buildFeeVariableList(bundle, feeCode));
        groups.put("formulas", new ArrayList<>(bundle.formulasByCode.values()));
        groups.put("rules", buildFeeRuleList(bundle, feeCode));
        groups.put("conditions", buildFeeConditionList(bundle, feeCode));
        groups.put("tiers", buildFeeTierList(bundle, feeCode));
        return groups;
    }

    public Map<String, Object> buildFeeDigest(PublishSnapshotBundle bundle, String feeCode) {
        if (bundle == null || !bundle.feesByCode.containsKey(feeCode)) {
            return null;
        }
        LinkedHashMap<String, Object> digest = new LinkedHashMap<>();
        digest.put("fee", bundle.feesByCode.get(feeCode));
        digest.put("rules", buildFeeRuleCompositeList(bundle, feeCode));
        digest.put("variables", buildFeeVariableList(bundle, feeCode));
        return digest;
    }

    public Map<String, Object> buildRuleComposite(PublishSnapshotBundle bundle, String ruleCode) {
        if (bundle == null || !bundle.rulesByCode.containsKey(ruleCode)) {
            return null;
        }
        LinkedHashMap<String, Object> composite = new LinkedHashMap<>();
        composite.put("rule", bundle.rulesByCode.get(ruleCode));
        composite.put("conditions", normalizeCollection(bundle.ruleConditionsByRuleCode.get(ruleCode)));
        composite.put("tiers", normalizeCollection(bundle.ruleTiersByRuleCode.get(ruleCode)));
        return composite;
    }

    public List<Map<String, Object>> buildFeeRuleCompositeList(PublishSnapshotBundle bundle, String feeCode) {
        bundle = normalizeBundle(bundle);
        List<Map<String, Object>> result = new ArrayList<>();
        for (String ruleCode : bundle.feeRuleCodes.getOrDefault(feeCode, new TreeSet<>())) {
            result.add(buildRuleComposite(bundle, ruleCode));
        }
        return result;
    }

    public int countFeeRuleChanges(PublishSnapshotBundle fromBundle, PublishSnapshotBundle toBundle, String feeCode) {
        Set<String> ruleCodes = new TreeSet<>();
        if (fromBundle != null) ruleCodes.addAll(fromBundle.feeRuleCodes.getOrDefault(feeCode, new TreeSet<>()));
        if (toBundle != null) ruleCodes.addAll(toBundle.feeRuleCodes.getOrDefault(feeCode, new TreeSet<>()));
        int count = 0;
        for (String ruleCode : ruleCodes) {
            if (!"UNCHANGED".equals(determineChangeType(buildRuleComposite(fromBundle, ruleCode), buildRuleComposite(toBundle, ruleCode)))) {
                count++;
            }
        }
        return count;
    }

    public int countFeeVariableChanges(PublishSnapshotBundle fromBundle, PublishSnapshotBundle toBundle, String feeCode) {
        Set<String> variableCodes = new TreeSet<>();
        if (fromBundle != null)
            variableCodes.addAll(fromBundle.feeReferencedVariables.getOrDefault(feeCode, new TreeSet<>()));
        if (toBundle != null)
            variableCodes.addAll(toBundle.feeReferencedVariables.getOrDefault(feeCode, new TreeSet<>()));
        int count = 0;
        for (String variableCode : variableCodes) {
            String fromJson = PublishJsonSupport.canonicalJson(fromBundle == null ? null : fromBundle.variablesByCode.get(variableCode));
            String toJson = PublishJsonSupport.canonicalJson(toBundle == null ? null : toBundle.variablesByCode.get(variableCode));
            if (!Objects.equals(fromJson, toJson)) {
                count++;
            }
        }
        return count;
    }

    public int countCollectionDiff(List<Map<String, Object>> fromList, List<Map<String, Object>> toList) {
        Set<String> fromSet = normalizeCollection(fromList).stream()
                .map(PublishJsonSupport::canonicalJson).collect(Collectors.toSet());
        Set<String> toSet = normalizeCollection(toList).stream()
                .map(PublishJsonSupport::canonicalJson).collect(Collectors.toSet());
        Set<String> merged = new LinkedHashSet<>(fromSet);
        merged.addAll(toSet);
        int sameCount = 0;
        for (String item : merged) {
            if (fromSet.contains(item) && toSet.contains(item)) {
                sameCount++;
            }
        }
        return merged.size() - sameCount;
    }

    public List<Map<String, Object>> buildFeeList(PublishSnapshotBundle bundle, String feeCode) {
        bundle = normalizeBundle(bundle);
        if (StringUtils.isNotEmpty(feeCode)) {
            return bundle.feesByCode.containsKey(feeCode) ? Collections.singletonList(bundle.feesByCode.get(feeCode)) : Collections.emptyList();
        }
        return new ArrayList<>(bundle.feesByCode.values());
    }

    public List<Map<String, Object>> buildFeeVariableList(PublishSnapshotBundle bundle, String feeCode) {
        bundle = normalizeBundle(bundle);
        if (StringUtils.isEmpty(feeCode)) {
            return new ArrayList<>(bundle.variablesByCode.values());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String variableCode : bundle.feeReferencedVariables.getOrDefault(feeCode, new TreeSet<>())) {
            Map<String, Object> variable = bundle.variablesByCode.get(variableCode);
            if (variable != null) {
                result.add(variable);
            }
        }
        return result;
    }

    public List<Map<String, Object>> buildFeeRuleList(PublishSnapshotBundle bundle, String feeCode) {
        bundle = normalizeBundle(bundle);
        if (StringUtils.isEmpty(feeCode)) {
            return new ArrayList<>(bundle.rulesByCode.values());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String ruleCode : bundle.feeRuleCodes.getOrDefault(feeCode, new TreeSet<>())) {
            Map<String, Object> rule = bundle.rulesByCode.get(ruleCode);
            if (rule != null) {
                result.add(rule);
            }
        }
        return result;
    }

    public List<Map<String, Object>> buildFeeConditionList(PublishSnapshotBundle bundle, String feeCode) {
        bundle = normalizeBundle(bundle);
        Collection<String> ruleCodes = StringUtils.isEmpty(feeCode) ? bundle.rulesByCode.keySet() : bundle.feeRuleCodes.getOrDefault(feeCode, new TreeSet<>());
        List<Map<String, Object>> result = new ArrayList<>();
        for (String ruleCode : ruleCodes) {
            result.addAll(bundle.ruleConditionsByRuleCode.getOrDefault(ruleCode, Collections.emptyList()));
        }
        return result;
    }

    public List<Map<String, Object>> buildFeeTierList(PublishSnapshotBundle bundle, String feeCode) {
        bundle = normalizeBundle(bundle);
        Collection<String> ruleCodes = StringUtils.isEmpty(feeCode) ? bundle.rulesByCode.keySet() : bundle.feeRuleCodes.getOrDefault(feeCode, new TreeSet<>());
        List<Map<String, Object>> result = new ArrayList<>();
        for (String ruleCode : ruleCodes) {
            result.addAll(bundle.ruleTiersByRuleCode.getOrDefault(ruleCode, Collections.emptyList()));
        }
        return result;
    }

    private String determineChangeType(Map<String, Object> fromObject, Map<String, Object> toObject) {
        if (fromObject == null && toObject == null) {
            return "UNCHANGED";
        }
        if (fromObject == null) {
            return "ADDED";
        }
        if (toObject == null) {
            return "REMOVED";
        }
        return Objects.equals(PublishJsonSupport.canonicalJson(fromObject),
                PublishJsonSupport.canonicalJson(toObject)) ? "UNCHANGED" : "CHANGED";
    }

    private List<String> compareChangedFields(Map<String, Object> fromObject, Map<String, Object> toObject) {
        Set<String> fields = new LinkedHashSet<>();
        if (fromObject != null) fields.addAll(fromObject.keySet());
        if (toObject != null) fields.addAll(toObject.keySet());
        List<String> changed = new ArrayList<>();
        for (String field : fields) {
            Object fromValue = fromObject == null ? null : fromObject.get(field);
            Object toValue = toObject == null ? null : toObject.get(field);
            if (!Objects.equals(PublishJsonSupport.canonicalJson(fromValue), PublishJsonSupport.canonicalJson(toValue))) {
                changed.add(field);
            }
        }
        return changed;
    }

    private String buildFeeSummaryText(String changeType, boolean feeDirectChanged, int ruleChangeCount, int variableChangeCount) {
        if ("ADDED".equals(changeType)) {
            return "该费用首次进入当前版本快照。";
        }
        if ("REMOVED".equals(changeType)) {
            return "该费用未再进入当前版本快照。";
        }
        List<String> parts = new ArrayList<>();
        if (feeDirectChanged) parts.add("费用主数据有变更");
        if (ruleChangeCount > 0) parts.add(String.format(Locale.ROOT, "%d条规则发生变化", ruleChangeCount));
        if (variableChangeCount > 0)
            parts.add(String.format(Locale.ROOT, "%d个引用变量口径发生变化", variableChangeCount));
        return parts.isEmpty() ? "费用快照有变化。" : String.join("，", parts) + "。";
    }

    private String resolveSceneFieldLabel(String field) {
        LinkedHashMap<String, String> labels = new LinkedHashMap<>();
        labels.put("sceneCode", "场景编码");
        labels.put("sceneName", "场景名称");
        labels.put("businessDomain", "业务域");
        labels.put("orgCode", "适用组织");
        labels.put("sceneType", "场景类型");
        labels.put("status", "状态");
        labels.put("remark", "说明");
        return labels.getOrDefault(field, field);
    }

    private List<Map<String, Object>> normalizeCollection(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream().sorted(Comparator.comparing(PublishJsonSupport::canonicalJson)).collect(Collectors.toList());
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.isNotEmpty(first) ? first : second;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
