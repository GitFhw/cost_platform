package com.ruoyi.system.service.impl.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.cost.CostFeeItem;
import com.ruoyi.system.domain.cost.CostFormula;
import com.ruoyi.system.domain.cost.CostPublishSnapshot;
import com.ruoyi.system.domain.cost.CostPublishVersion;
import com.ruoyi.system.domain.cost.CostRule;
import com.ruoyi.system.domain.cost.CostScene;
import com.ruoyi.system.domain.cost.CostVariable;
import com.ruoyi.system.domain.cost.bo.CostPublishCreateBo;
import com.ruoyi.system.domain.vo.CostPublishCheckItemVo;
import com.ruoyi.system.mapper.cost.CostFeeMapper;
import com.ruoyi.system.mapper.cost.CostFormulaMapper;
import com.ruoyi.system.mapper.cost.CostPublishSnapshotMapper;
import com.ruoyi.system.mapper.cost.CostPublishVersionMapper;
import com.ruoyi.system.mapper.cost.CostRuleMapper;
import com.ruoyi.system.mapper.cost.CostSceneMapper;
import com.ruoyi.system.mapper.cost.CostSimulationRecordMapper;
import com.ruoyi.system.mapper.cost.CostVariableMapper;
import com.ruoyi.system.service.cost.ICostPublishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 发布中心服务实现
 *
 * @author codex
 */
@Service
public class CostPublishServiceImpl implements ICostPublishService
{
    private static final String STATUS_ENABLED = "0";
    private static final String VERSION_STATUS_PUBLISHED = "PUBLISHED";
    private static final String VERSION_STATUS_ACTIVE = "ACTIVE";
    private static final String VERSION_STATUS_ROLLED_BACK = "ROLLED_BACK";
    private static final String SIMULATION_STATUS_SUCCESS = "SUCCESS";
    private static final String SNAPSHOT_SCENE = "SCENE";
    private static final String SNAPSHOT_FEE = "FEE";
    private static final String SNAPSHOT_VARIABLE = "VARIABLE";
    private static final String SNAPSHOT_FORMULA = "FORMULA";
    private static final String SNAPSHOT_RULE = "RULE";
    private static final String SNAPSHOT_RULE_CONDITION = "RULE_CONDITION";
    private static final String SNAPSHOT_RULE_TIER = "RULE_TIER";
    private static final DecimalFormat VERSION_SEQ_FORMAT = new DecimalFormat("000");

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    @Autowired
    private CostPublishVersionMapper publishVersionMapper;

    @Autowired
    private CostPublishSnapshotMapper publishSnapshotMapper;

    @Autowired
    private CostSceneMapper sceneMapper;

    @Autowired
    private CostFeeMapper feeMapper;

    @Autowired
    private CostVariableMapper variableMapper;

    @Autowired
    private CostFormulaMapper formulaMapper;

    @Autowired
    private CostRuleMapper ruleMapper;

    @Autowired
    private CostSimulationRecordMapper simulationRecordMapper;

    @Override
    public Map<String, Object> selectPublishStats(CostPublishVersion query)
    {
        Map<String, Object> stats = publishVersionMapper.selectPublishStats(query);
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("sceneCount", getLongValue(stats, "sceneCount"));
        result.put("versionCount", getLongValue(stats, "versionCount"));
        result.put("activeVersionCount", getLongValue(stats, "activeVersionCount"));
        result.put("rolledBackVersionCount", getLongValue(stats, "rolledBackVersionCount"));
        result.put("latestWarningCount", 0);
        result.put("latestBlockingCount", 0);
        result.put("latestCheckResult", "UNAVAILABLE");
        result.put("latestCheckLabel", "暂无校验");
        if (query != null && query.getSceneId() != null)
        {
            CostPublishVersion activeVersion = publishVersionMapper.selectActiveVersionByScene(query.getSceneId());
            CostPublishVersion latestVersion = publishVersionMapper.selectLatestVersionByScene(query.getSceneId());
            result.put("activeVersionId", activeVersion == null ? null : activeVersion.getVersionId());
            result.put("activeVersionNo", activeVersion == null ? null : activeVersion.getVersionNo());
            result.put("latestVersionId", latestVersion == null ? null : latestVersion.getVersionId());
            result.put("latestVersionNo", latestVersion == null ? null : latestVersion.getVersionNo());
            Map<String, Object> latestValidation = latestVersion == null ? Collections.emptyMap() : parseJsonMap(latestVersion.getValidationResultJson());
            long warningCount = getLongValue(latestValidation, "warningCount");
            long blockingCount = getLongValue(latestValidation, "blockingCount");
            result.put("latestWarningCount", warningCount);
            result.put("latestBlockingCount", blockingCount);
            result.put("latestCheckResult", resolveCheckResult(blockingCount, warningCount, latestVersion));
            result.put("latestCheckLabel", resolveCheckLabel(blockingCount, warningCount, latestVersion));
        }
        return result;
    }

    @Override
    public Map<String, Object> selectPublishPrecheck(Long sceneId)
    {
        SnapshotBundle draftBundle = buildDraftSnapshotBundle(sceneId);
        CostPublishVersion activeVersion = publishVersionMapper.selectActiveVersionByScene(sceneId);
        SnapshotBundle activeBundle = normalizeBundle(activeVersion == null ? null : loadSnapshotBundle(activeVersion.getVersionId()));
        CostPublishVersion latestVersion = publishVersionMapper.selectLatestVersionByScene(sceneId);

        List<CostPublishCheckItemVo> items = new ArrayList<>();
        appendCheckItems(sceneId, draftBundle, activeVersion, activeBundle, items);
        long blockingCount = items.stream().filter(item -> "BLOCK".equals(item.getLevel())).count();
        long warningCount = items.stream().filter(item -> "WARN".equals(item.getLevel())).count();
        List<Map<String, Object>> impactedFees = buildFeeDiffSummary(activeBundle, draftBundle, null);

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("sceneId", draftBundle.sceneId);
        result.put("sceneCode", draftBundle.sceneCode);
        result.put("sceneName", draftBundle.sceneName);
        result.put("publishedVersionCount", publishVersionMapper.selectCount(Wrappers.<CostPublishVersion>lambdaQuery()
                .eq(CostPublishVersion::getSceneId, sceneId)));
        result.put("activeVersionId", activeVersion == null ? null : activeVersion.getVersionId());
        result.put("activeVersionNo", activeVersion == null ? null : activeVersion.getVersionNo());
        result.put("latestVersionId", latestVersion == null ? null : latestVersion.getVersionId());
        result.put("latestVersionNo", latestVersion == null ? null : latestVersion.getVersionNo());
        result.put("draftSnapshotHash", draftBundle.snapshotHash);
        result.put("activeSnapshotHash", activeVersion == null ? null : activeVersion.getSnapshotHash());
        result.put("publishable", blockingCount <= 0);
        result.put("blockingCount", blockingCount);
        result.put("warningCount", warningCount);
        result.put("feeCount", draftBundle.feesByCode.size());
        result.put("variableCount", draftBundle.variablesByCode.size());
        result.put("ruleCount", draftBundle.rulesByCode.size());
        result.put("conditionCount", draftBundle.ruleConditionsByRuleCode.values().stream().mapToInt(List::size).sum());
        result.put("tierCount", draftBundle.ruleTiersByRuleCode.values().stream().mapToInt(List::size).sum());
        result.put("items", items);
        result.put("impactedFeeCount", impactedFees.size());
        result.put("impactedFees", impactedFees);
        result.put("suggestActivateNow", activeVersion == null);
        return result;
    }

    @Override
    public List<CostPublishVersion> selectPublishVersionList(CostPublishVersion query)
    {
        return publishVersionMapper.selectPublishVersionList(query);
    }

    @Override
    public Map<String, Object> selectPublishVersionDetail(Long versionId, String feeCode)
    {
        CostPublishVersion version = requireVersion(versionId);
        SnapshotBundle currentBundle = normalizeBundle(loadSnapshotBundle(versionId));
        CostPublishVersion previousVersion = publishVersionMapper.selectPreviousVersion(version.getSceneId(), versionId);
        SnapshotBundle previousBundle = normalizeBundle(previousVersion == null ? null : loadSnapshotBundle(previousVersion.getVersionId()));

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("version", version);
        result.put("previousVersionId", previousVersion == null ? null : previousVersion.getVersionId());
        result.put("previousVersionNo", previousVersion == null ? null : previousVersion.getVersionNo());
        result.put("validationResult", parseJsonMap(version.getValidationResultJson()));
        result.put("impactedFees", buildFeeDiffSummary(previousBundle, currentBundle, feeCode));
        result.put("snapshotCounts", buildSnapshotCounts(currentBundle, feeCode));
        result.put("snapshotGroups", buildSnapshotGroups(currentBundle, feeCode));
        return result;
    }

    @Override
    public Map<String, Object> selectPublishDiff(Long fromVersionId, Long toVersionId, String feeCode)
    {
        CostPublishVersion fromVersion = requireVersion(fromVersionId);
        CostPublishVersion toVersion = requireVersion(toVersionId);
        if (!Objects.equals(fromVersion.getSceneId(), toVersion.getSceneId()))
        {
            throw new ServiceException("版本差异只能在同一场景下比较");
        }
        SnapshotBundle fromBundle = normalizeBundle(loadSnapshotBundle(fromVersionId));
        SnapshotBundle toBundle = normalizeBundle(loadSnapshotBundle(toVersionId));
        List<Map<String, Object>> feeDiffs = buildFeeDiffSummary(fromBundle, toBundle, feeCode);
        List<Map<String, Object>> ruleDiffs = buildRuleDiffSummary(fromBundle, toBundle, feeCode);
        List<Map<String, Object>> sceneDiffs = buildSceneDiffSummary(fromBundle.sceneSnapshot, toBundle.sceneSnapshot);

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("sceneChangeCount", sceneDiffs.size());
        summary.put("feeChangeCount", feeDiffs.size());
        summary.put("addedFeeCount", feeDiffs.stream().filter(item -> "ADDED".equals(item.get("changeType"))).count());
        summary.put("removedFeeCount", feeDiffs.stream().filter(item -> "REMOVED".equals(item.get("changeType"))).count());
        summary.put("changedFeeCount", feeDiffs.stream().filter(item -> "CHANGED".equals(item.get("changeType"))).count());
        summary.put("ruleChangeCount", ruleDiffs.size());

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("fromVersion", fromVersion);
        result.put("toVersion", toVersion);
        result.put("summary", summary);
        result.put("sceneDiffs", sceneDiffs);
        result.put("feeDiffs", feeDiffs);
        result.put("ruleDiffs", ruleDiffs);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int publishScene(CostPublishCreateBo bo)
    {
        Map<String, Object> precheck = selectPublishPrecheck(bo.getSceneId());
        if (!Boolean.TRUE.equals(precheck.get("publishable")))
        {
            throw new ServiceException("当前场景存在阻断项，请先处理后再发布");
        }

        SnapshotBundle draftBundle = buildDraftSnapshotBundle(bo.getSceneId());
        String operator = SecurityUtils.getUsername();
        Date now = DateUtils.getNowDate();

        CostPublishVersion version = new CostPublishVersion();
        version.setSceneId(bo.getSceneId());
        version.setVersionNo(buildNextVersionNo(bo.getSceneId(), now));
        version.setVersionStatus(VERSION_STATUS_PUBLISHED);
        version.setPublishDesc(bo.getPublishDesc());
        version.setValidationResultJson(writeJson(precheck));
        version.setSnapshotHash(draftBundle.snapshotHash);
        version.setPublishedBy(operator);
        version.setPublishedTime(now);
        publishVersionMapper.insert(version);

        for (CostPublishSnapshot snapshot : draftBundle.snapshotRows)
        {
            snapshot.setSnapshotId(null);
            snapshot.setVersionId(version.getVersionId());
            publishSnapshotMapper.insert(snapshot);
        }

        if (Boolean.TRUE.equals(bo.getActivateNow()))
        {
            activateVersionInternal(version, false, operator, now);
        }
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int activateVersion(Long versionId)
    {
        activateVersionInternal(requireVersion(versionId), false, SecurityUtils.getUsername(), DateUtils.getNowDate());
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int rollbackVersion(Long versionId)
    {
        activateVersionInternal(requireVersion(versionId), true, SecurityUtils.getUsername(), DateUtils.getNowDate());
        return 1;
    }

    private void appendCheckItems(Long sceneId, SnapshotBundle draftBundle, CostPublishVersion activeVersion,
            SnapshotBundle activeBundle, List<CostPublishCheckItemVo> items)
    {
        if (!STATUS_ENABLED.equals(String.valueOf(draftBundle.sceneSnapshot.get("status"))))
        {
            items.add(createCheckItem("BLOCK", "SCENE_DISABLED", "场景状态检查", "当前场景已停用，不能发布新版本。"));
        }
        else
        {
            items.add(createCheckItem("PASS", "SCENE_STATUS_OK", "场景状态检查", "当前场景状态正常，可进入发布治理。"));
        }
        if (draftBundle.feesByCode.isEmpty())
        {
            items.add(createCheckItem("BLOCK", "NO_FEE", "费用完整性", "当前场景下暂无启用费用，无法形成可运行版本。"));
        }
        if (draftBundle.variablesByCode.isEmpty())
        {
            items.add(createCheckItem("BLOCK", "NO_VARIABLE", "变量完整性", "当前场景下暂无启用变量，无法形成稳定的规则运行口径。"));
        }
        if (draftBundle.rulesByCode.isEmpty())
        {
            items.add(createCheckItem("BLOCK", "NO_RULE", "规则完整性", "当前场景下暂无启用规则，无法发布可运行版本。"));
        }

        List<Map<String, Object>> feesWithoutRule = publishVersionMapper.selectEnabledFeesWithoutRule(sceneId);
        if (feesWithoutRule.isEmpty())
        {
            items.add(createCheckItem("PASS", "FEE_RULE_COVERED", "费用规则覆盖", "所有启用费用都已挂载至少一条启用规则。"));
        }
        else
        {
            String joined = feesWithoutRule.stream()
                    .map(item -> String.format("%1$s(%2$s)", item.get("feeName"), item.get("feeCode")))
                    .collect(Collectors.joining("、"));
            items.add(createCheckItem("BLOCK", "FEE_RULE_MISSING", "费用规则覆盖", "以下费用尚未挂载启用规则：" + joined));
        }

        List<Map<String, Object>> tierRuleWithoutTier = publishVersionMapper.selectTierRulesWithoutTier(sceneId);
        if (tierRuleWithoutTier.isEmpty())
        {
            items.add(createCheckItem("PASS", "RULE_TIER_OK", "阶梯结构检查", "当前场景的阶梯规则都已具备完整阶梯明细。"));
        }
        else
        {
            String joined = tierRuleWithoutTier.stream()
                    .map(item -> String.format("%1$s(%2$s)", item.get("ruleName"), item.get("ruleCode")))
                    .collect(Collectors.joining("、"));
            items.add(createCheckItem("BLOCK", "RULE_TIER_MISSING", "阶梯结构检查", "以下阶梯规则缺少阶梯明细：" + joined));
        }

        appendFormulaReferenceChecks(draftBundle, items);
        if (activeVersion == null)
        {
            items.add(createCheckItem("WARN", "FIRST_RELEASE", "首发提醒", "当前场景还没有生效版本，本次发布后建议尽快设为生效。"));
            appendFirstReleaseSimulationCheck(sceneId, items);
        }
        else if (StringUtils.equals(draftBundle.snapshotHash, activeVersion.getSnapshotHash()))
        {
            items.add(createCheckItem("WARN", "SNAPSHOT_NO_CHANGE", "版本差异提醒",
                    String.format("当前草稿与生效版本%1$s的快照哈希一致，发布后可能不会产生业务差异。", activeVersion.getVersionNo())));
        }
        else
        {
            int impactedFeeCount = buildFeeDiffSummary(activeBundle, draftBundle, null).size();
            items.add(createCheckItem("PASS", "SNAPSHOT_CHANGED", "版本差异提醒",
                    String.format(Locale.ROOT, "当前草稿相较生效版本%1$s识别到%2$d个受影响费用。", activeVersion.getVersionNo(), impactedFeeCount)));
        }
    }

    /**
     * 首发发布时补充试算提醒。
     *
     * <p>首发缺少成功试算记录只做警告，不阻断发布，以便业务人员先感知风险、
     * 再决定是否继续发布。</p>
     *
     * @param sceneId 场景主键
     * @param items 校验项集合
     */
    private void appendFirstReleaseSimulationCheck(Long sceneId, List<CostPublishCheckItemVo> items)
    {
        long successSimulationCount = simulationRecordMapper.selectCount(Wrappers.lambdaQuery(com.ruoyi.system.domain.cost.CostSimulationRecord.class)
                .eq(com.ruoyi.system.domain.cost.CostSimulationRecord::getSceneId, sceneId)
                .eq(com.ruoyi.system.domain.cost.CostSimulationRecord::getStatus, SIMULATION_STATUS_SUCCESS));
        if (successSimulationCount > 0)
        {
            items.add(createCheckItem("PASS", "FIRST_RELEASE_SIMULATION_READY", "试算提醒",
                    String.format(Locale.ROOT, "当前场景已有%1$d条成功试算记录，可作为首次发布前的业务核对依据。", successSimulationCount)));
            return;
        }
        items.add(createCheckItem("WARN", "FIRST_RELEASE_NO_SIMULATION", "试算提醒",
                "当前场景为首次发布，且尚无成功试算记录，建议先完成至少1次试算核对再发布。"));
    }

    /**
     * 公式编码治理检查。
     *
     * <p>新配置应以公式编码为唯一入口。发布前再做一次收口，
     * 避免仅存原始表达式的历史兼容数据继续进入新快照。</p>
     */
    private void appendFormulaReferenceChecks(SnapshotBundle draftBundle, List<CostPublishCheckItemVo> items)
    {
        List<String> variableMissingCode = new ArrayList<>();
        List<String> variableMissingAsset = new ArrayList<>();
        int formulaVariableCount = 0;
        for (Map<String, Object> variable : draftBundle.variablesByCode.values())
        {
            if (!"FORMULA".equalsIgnoreCase(stringValue(variable.get("sourceType"))))
            {
                continue;
            }
            formulaVariableCount++;
            String formulaCode = StringUtils.trim(stringValue(variable.get("formulaCode")));
            if (StringUtils.isEmpty(formulaCode))
            {
                variableMissingCode.add(buildAssetLabel(variable, "variableName", "variableCode"));
                continue;
            }
            if (!draftBundle.formulasByCode.containsKey(formulaCode))
            {
                variableMissingAsset.add(buildAssetLabel(variable, "variableName", "variableCode") + " -> " + formulaCode);
            }
        }

        List<String> ruleMissingCode = new ArrayList<>();
        List<String> ruleMissingAsset = new ArrayList<>();
        int formulaRuleCount = 0;
        for (Map<String, Object> rule : draftBundle.rulesByCode.values())
        {
            if (!"FORMULA".equalsIgnoreCase(stringValue(rule.get("ruleType"))))
            {
                continue;
            }
            formulaRuleCount++;
            String formulaCode = StringUtils.trim(stringValue(rule.get("amountFormulaCode")));
            if (StringUtils.isEmpty(formulaCode))
            {
                ruleMissingCode.add(buildAssetLabel(rule, "ruleName", "ruleCode"));
                continue;
            }
            if (!draftBundle.formulasByCode.containsKey(formulaCode))
            {
                ruleMissingAsset.add(buildAssetLabel(rule, "ruleName", "ruleCode") + " -> " + formulaCode);
            }
        }

        if (!variableMissingCode.isEmpty())
        {
            items.add(createCheckItem("BLOCK", "FORMULA_VARIABLE_CODE_MISSING", "公式编码治理",
                    "以下公式变量仍未绑定公式编码，发布前请先在公式实验室沉淀后重新选择：" + String.join("、", variableMissingCode)));
        }
        if (!variableMissingAsset.isEmpty())
        {
            items.add(createCheckItem("BLOCK", "FORMULA_VARIABLE_ASSET_MISSING", "公式编码治理",
                    "以下公式变量引用的公式编码当前场景不存在或未启用：" + String.join("、", variableMissingAsset)));
        }
        if (!ruleMissingCode.isEmpty())
        {
            items.add(createCheckItem("BLOCK", "FORMULA_RULE_CODE_MISSING", "公式编码治理",
                    "以下公式金额规则仍未绑定金额公式编码，发布前请先在公式实验室选择金额公式：" + String.join("、", ruleMissingCode)));
        }
        if (!ruleMissingAsset.isEmpty())
        {
            items.add(createCheckItem("BLOCK", "FORMULA_RULE_ASSET_MISSING", "公式编码治理",
                    "以下公式金额规则引用的金额公式编码当前场景不存在或未启用：" + String.join("、", ruleMissingAsset)));
        }

        if (variableMissingCode.isEmpty() && variableMissingAsset.isEmpty() && ruleMissingCode.isEmpty() && ruleMissingAsset.isEmpty())
        {
            if (formulaVariableCount > 0 || formulaRuleCount > 0)
            {
                items.add(createCheckItem("PASS", "FORMULA_REFERENCE_OK", "公式编码治理",
                        String.format(Locale.ROOT, "当前场景 %1$d 个公式变量、%2$d 条公式金额规则均已绑定有效公式编码。", formulaVariableCount, formulaRuleCount)));
            }
            else
            {
                items.add(createCheckItem("PASS", "FORMULA_REFERENCE_EMPTY", "公式编码治理", "当前场景暂无公式变量或公式金额规则，无需执行公式编码治理检查。"));
            }
        }
    }

    private void activateVersionInternal(CostPublishVersion targetVersion, boolean rollback, String operator, Date operateTime)
    {
        CostScene scene = sceneMapper.selectById(targetVersion.getSceneId());
        if (StringUtils.isNull(scene))
        {
            throw new ServiceException("所属场景不存在，无法切换版本");
        }
        CostPublishVersion currentActive = publishVersionMapper.selectActiveVersionByScene(targetVersion.getSceneId());
        if (currentActive != null && Objects.equals(currentActive.getVersionId(), targetVersion.getVersionId()))
        {
            if (rollback)
            {
                throw new ServiceException("褰撳墠鐗堟湰宸茬粡鏄敓鏁堢増鏈紝涓嶉渶鍥炴粴");
            }
            return;
        }
        if (!rollback && !StringUtils.equalsAny(targetVersion.getVersionStatus(), VERSION_STATUS_PUBLISHED, VERSION_STATUS_ROLLED_BACK))
        {
            throw new ServiceException("鍙湁宸插彂甯冩垨宸插洖婊氱殑鐗堟湰鎵嶈兘璁句负鐢熸晥");
        }
        if (rollback && !StringUtils.equalsAny(targetVersion.getVersionStatus(), VERSION_STATUS_PUBLISHED, VERSION_STATUS_ROLLED_BACK))
        {
            throw new ServiceException("鍙湁鍘嗗彶鍙戝竷鐗堟湰鎵嶈兘浣滀负鍥炴粴鐩爣");
        }
        if (currentActive != null)
        {
            publishVersionMapper.update(null, Wrappers.<CostPublishVersion>lambdaUpdate()
                    .eq(CostPublishVersion::getVersionId, currentActive.getVersionId())
                    .set(CostPublishVersion::getVersionStatus, rollback ? VERSION_STATUS_ROLLED_BACK : VERSION_STATUS_PUBLISHED)
                    .set(rollback, CostPublishVersion::getRollbackBy, operator)
                    .set(rollback, CostPublishVersion::getRollbackTime, operateTime)
                    .set(CostPublishVersion::getUpdateBy, operator)
                    .set(CostPublishVersion::getUpdateTime, operateTime));
        }
        publishVersionMapper.update(null, Wrappers.<CostPublishVersion>lambdaUpdate()
                .eq(CostPublishVersion::getVersionId, targetVersion.getVersionId())
                .set(CostPublishVersion::getVersionStatus, VERSION_STATUS_ACTIVE)
                .set(CostPublishVersion::getActivatedBy, operator)
                .set(CostPublishVersion::getActivatedTime, operateTime)
                .set(CostPublishVersion::getUpdateBy, operator)
                .set(CostPublishVersion::getUpdateTime, operateTime));
        sceneMapper.update(null, Wrappers.<CostScene>lambdaUpdate()
                .eq(CostScene::getSceneId, targetVersion.getSceneId())
                .set(CostScene::getActiveVersionId, targetVersion.getVersionId())
                .set(CostScene::getUpdateBy, operator)
                .set(CostScene::getUpdateTime, operateTime));
    }

    private SnapshotBundle buildDraftSnapshotBundle(Long sceneId)
    {
        CostScene scene = sceneMapper.selectById(sceneId);
        if (StringUtils.isNull(scene))
        {
            throw new ServiceException("场景不存在，请重新选择");
        }

        CostFeeItem feeQuery = new CostFeeItem();
        feeQuery.setSceneId(sceneId);
        feeQuery.setStatus(STATUS_ENABLED);
        List<CostFeeItem> feeItems = feeMapper.selectFeeOptions(feeQuery);
        Map<Long, CostFeeItem> feeById = feeItems.stream().collect(Collectors.toMap(CostFeeItem::getFeeId, item -> item, (a, b) -> a, LinkedHashMap::new));

        CostVariable variableQuery = new CostVariable();
        variableQuery.setSceneId(sceneId);
        variableQuery.setStatus(STATUS_ENABLED);
        List<CostVariable> variables = variableMapper.selectVariableOptions(variableQuery);

        CostFormula formulaQuery = new CostFormula();
        formulaQuery.setSceneId(sceneId);
        formulaQuery.setStatus(STATUS_ENABLED);
        List<CostFormula> formulas = formulaMapper.selectFormulaOptions(formulaQuery);

        CostRule ruleQuery = new CostRule();
        ruleQuery.setSceneId(sceneId);
        ruleQuery.setStatus(STATUS_ENABLED);
        List<CostRule> rules = ruleMapper.selectRuleList(ruleQuery).stream()
                .filter(item -> feeById.containsKey(item.getFeeId()))
                .sorted(Comparator.comparing(CostRule::getRuleCode, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());

        SnapshotBundle bundle = new SnapshotBundle();
        bundle.sceneId = scene.getSceneId();
        bundle.sceneCode = scene.getSceneCode();
        bundle.sceneName = scene.getSceneName();
        bundle.sceneSnapshot = buildSceneSnapshot(scene);
        addSceneSnapshot(bundle, scene);
        addFeeSnapshots(bundle, feeItems);
        addVariableSnapshots(bundle, variables);
        addFormulaSnapshots(bundle, formulas);
        addRuleSnapshots(bundle, rules);
        addConditionSnapshots(bundle, publishVersionMapper.selectRuleConditionsForPublish(sceneId));
        addTierSnapshots(bundle, publishVersionMapper.selectRuleTiersForPublish(sceneId));
        bundle.snapshotHash = buildSnapshotHash(bundle.snapshotRows);
        return bundle;
    }

    private SnapshotBundle loadSnapshotBundle(Long versionId)
    {
        CostPublishVersion version = requireVersion(versionId);
        SnapshotBundle bundle = new SnapshotBundle();
        bundle.sceneId = version.getSceneId();
        bundle.sceneCode = version.getSceneCode();
        bundle.sceneName = version.getSceneName();
        bundle.snapshotHash = version.getSnapshotHash();
        bundle.snapshotRows = publishVersionMapper.selectSnapshotList(versionId, null);
        for (CostPublishSnapshot snapshot : bundle.snapshotRows)
        {
            Map<String, Object> json = parseJsonMap(snapshot.getSnapshotJson());
            if (SNAPSHOT_SCENE.equals(snapshot.getSnapshotType()))
            {
                bundle.sceneSnapshot = json;
            }
            else if (SNAPSHOT_FEE.equals(snapshot.getSnapshotType()))
            {
                bundle.feesByCode.put(snapshot.getObjectCode(), json);
            }
            else if (SNAPSHOT_VARIABLE.equals(snapshot.getSnapshotType()))
            {
                bundle.variablesByCode.put(snapshot.getObjectCode(), json);
            }
            else if (SNAPSHOT_FORMULA.equals(snapshot.getSnapshotType()))
            {
                bundle.formulasByCode.put(snapshot.getObjectCode(), json);
            }
            else if (SNAPSHOT_RULE.equals(snapshot.getSnapshotType()))
            {
                bundle.rulesByCode.put(snapshot.getObjectCode(), json);
                String feeCode = stringValue(json.get("feeCode"));
                bundle.ruleToFeeCode.put(snapshot.getObjectCode(), feeCode);
                bundle.feeRuleCodes.computeIfAbsent(feeCode, key -> new TreeSet<>()).add(snapshot.getObjectCode());
                addRefVariable(bundle, feeCode, stringValue(json.get("quantityVariableCode")));
            }
            else if (SNAPSHOT_RULE_CONDITION.equals(snapshot.getSnapshotType()))
            {
                String ruleCode = stringValue(json.get("ruleCode"));
                String feeCode = stringValue(json.get("feeCode"));
                bundle.ruleConditionsByRuleCode.computeIfAbsent(ruleCode, key -> new ArrayList<>()).add(json);
                addRefVariable(bundle, feeCode, stringValue(json.get("variableCode")));
            }
            else if (SNAPSHOT_RULE_TIER.equals(snapshot.getSnapshotType()))
            {
                String ruleCode = stringValue(json.get("ruleCode"));
                bundle.ruleTiersByRuleCode.computeIfAbsent(ruleCode, key -> new ArrayList<>()).add(json);
            }
        }
        sortBundleCollections(bundle);
        return bundle;
    }

    private void addSceneSnapshot(SnapshotBundle bundle, CostScene scene)
    {
        CostPublishSnapshot snapshot = new CostPublishSnapshot();
        snapshot.setSnapshotType(SNAPSHOT_SCENE);
        snapshot.setObjectCode(scene.getSceneCode());
        snapshot.setObjectName(scene.getSceneName());
        snapshot.setSnapshotJson(writeJson(bundle.sceneSnapshot));
        snapshot.setSortNo(1);
        bundle.snapshotRows.add(snapshot);
    }

    private void addFeeSnapshots(SnapshotBundle bundle, List<CostFeeItem> feeItems)
    {
        int sortNo = 10;
        for (CostFeeItem fee : feeItems)
        {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("feeCode", fee.getFeeCode());
            json.put("feeName", fee.getFeeName());
            json.put("feeCategory", fee.getFeeCategory());
            json.put("unitCode", fee.getUnitCode());
            json.put("factorSummary", fee.getFactorSummary());
            json.put("scopeDescription", fee.getScopeDescription());
            json.put("objectDimension", fee.getObjectDimension());
            json.put("sortNo", fee.getSortNo());
            json.put("status", fee.getStatus());
            json.put("remark", fee.getRemark());
            bundle.feesByCode.put(fee.getFeeCode(), json);

            CostPublishSnapshot snapshot = new CostPublishSnapshot();
            snapshot.setSnapshotType(SNAPSHOT_FEE);
            snapshot.setObjectCode(fee.getFeeCode());
            snapshot.setObjectName(fee.getFeeName());
            snapshot.setSnapshotJson(writeJson(json));
            snapshot.setSortNo(sortNo++);
            bundle.snapshotRows.add(snapshot);
        }
    }

    private void addVariableSnapshots(SnapshotBundle bundle, List<CostVariable> variables)
    {
        int sortNo = 1000;
        for (CostVariable variable : variables)
        {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("variableCode", variable.getVariableCode());
            json.put("variableName", variable.getVariableName());
            json.put("variableType", variable.getVariableType());
            json.put("sourceType", variable.getSourceType());
            json.put("dictType", variable.getDictType());
            json.put("remoteApi", variable.getRemoteApi());
            json.put("dataPath", variable.getDataPath());
            json.put("formulaExpr", variable.getFormulaExpr());
            json.put("formulaCode", variable.getFormulaCode());
            json.put("dataType", variable.getDataType());
            json.put("defaultValue", variable.getDefaultValue());
            json.put("precisionScale", variable.getPrecisionScale());
            json.put("sortNo", variable.getSortNo());
            json.put("status", variable.getStatus());
            json.put("remark", variable.getRemark());
            bundle.variablesByCode.put(variable.getVariableCode(), json);

            CostPublishSnapshot snapshot = new CostPublishSnapshot();
            snapshot.setSnapshotType(SNAPSHOT_VARIABLE);
            snapshot.setObjectCode(variable.getVariableCode());
            snapshot.setObjectName(variable.getVariableName());
            snapshot.setSnapshotJson(writeJson(json));
            snapshot.setSortNo(sortNo++);
            bundle.snapshotRows.add(snapshot);
        }
    }

    private void addFormulaSnapshots(SnapshotBundle bundle, List<CostFormula> formulas)
    {
        int sortNo = 1500;
        for (CostFormula formula : formulas)
        {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("formulaCode", formula.getFormulaCode());
            json.put("formulaName", formula.getFormulaName());
            json.put("formulaDesc", formula.getFormulaDesc());
            json.put("businessFormula", formula.getBusinessFormula());
            json.put("formulaExpr", formula.getFormulaExpr());
            json.put("namespaceScope", formula.getNamespaceScope());
            json.put("returnType", formula.getReturnType());
            json.put("sortNo", formula.getSortNo());
            json.put("status", formula.getStatus());
            json.put("remark", formula.getRemark());
            bundle.formulasByCode.put(formula.getFormulaCode(), json);

            CostPublishSnapshot snapshot = new CostPublishSnapshot();
            snapshot.setSnapshotType(SNAPSHOT_FORMULA);
            snapshot.setObjectCode(formula.getFormulaCode());
            snapshot.setObjectName(formula.getFormulaName());
            snapshot.setSnapshotJson(writeJson(json));
            snapshot.setSortNo(sortNo++);
            bundle.snapshotRows.add(snapshot);
        }
    }

    private void addRuleSnapshots(SnapshotBundle bundle, List<CostRule> rules)
    {
        int sortNo = 2000;
        for (CostRule rule : rules)
        {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("ruleCode", rule.getRuleCode());
            json.put("ruleName", rule.getRuleName());
            json.put("feeCode", rule.getFeeCode());
            json.put("feeName", rule.getFeeName());
            json.put("ruleType", rule.getRuleType());
            json.put("conditionLogic", rule.getConditionLogic());
            json.put("priority", rule.getPriority());
            json.put("quantityVariableCode", rule.getQuantityVariableCode());
            json.put("pricingMode", rule.getPricingMode());
            json.put("pricingJson", parseJsonMap(rule.getPricingJson()));
            json.put("amountFormula", rule.getAmountFormula());
            json.put("amountFormulaCode", rule.getAmountFormulaCode());
            json.put("amountBusinessFormula", rule.getAmountBusinessFormula());
            json.put("noteTemplate", rule.getNoteTemplate());
            json.put("sortNo", rule.getSortNo());
            json.put("status", rule.getStatus());
            json.put("remark", rule.getRemark());
            bundle.rulesByCode.put(rule.getRuleCode(), json);
            bundle.ruleToFeeCode.put(rule.getRuleCode(), rule.getFeeCode());
            bundle.feeRuleCodes.computeIfAbsent(rule.getFeeCode(), key -> new TreeSet<>()).add(rule.getRuleCode());
            addRefVariable(bundle, rule.getFeeCode(), rule.getQuantityVariableCode());

            CostPublishSnapshot snapshot = new CostPublishSnapshot();
            snapshot.setSnapshotType(SNAPSHOT_RULE);
            snapshot.setObjectCode(rule.getRuleCode());
            snapshot.setObjectName(rule.getRuleName());
            snapshot.setSnapshotJson(writeJson(json));
            snapshot.setSortNo(sortNo++);
            bundle.snapshotRows.add(snapshot);
        }
    }

    private void addConditionSnapshots(SnapshotBundle bundle, List<Map<String, Object>> rows)
    {
        int sortNo = 3000;
        for (Map<String, Object> source : rows)
        {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("ruleCode", source.get("ruleCode"));
            json.put("ruleName", source.get("ruleName"));
            json.put("feeCode", source.get("feeCode"));
            json.put("feeName", source.get("feeName"));
            json.put("groupNo", source.get("groupNo"));
            json.put("sortNo", source.get("sortNo"));
            json.put("variableCode", source.get("variableCode"));
            json.put("displayName", source.get("displayName"));
            json.put("operatorCode", source.get("operatorCode"));
            json.put("compareValue", source.get("compareValue"));
            json.put("status", source.get("status"));
            json.put("remark", source.get("remark"));

            String ruleCode = stringValue(source.get("ruleCode"));
            String feeCode = stringValue(source.get("feeCode"));
            bundle.ruleConditionsByRuleCode.computeIfAbsent(ruleCode, key -> new ArrayList<>()).add(json);
            addRefVariable(bundle, feeCode, stringValue(source.get("variableCode")));

            CostPublishSnapshot snapshot = new CostPublishSnapshot();
            snapshot.setSnapshotType(SNAPSHOT_RULE_CONDITION);
            snapshot.setObjectCode(ruleCode + "#C" + source.get("sortNo"));
            snapshot.setObjectName(stringValue(source.get("displayName")));
            snapshot.setSnapshotJson(writeJson(json));
            snapshot.setSortNo(sortNo++);
            bundle.snapshotRows.add(snapshot);
        }
    }

    private void addTierSnapshots(SnapshotBundle bundle, List<Map<String, Object>> rows)
    {
        int sortNo = 4000;
        for (Map<String, Object> source : rows)
        {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("ruleCode", source.get("ruleCode"));
            json.put("ruleName", source.get("ruleName"));
            json.put("feeCode", source.get("feeCode"));
            json.put("feeName", source.get("feeName"));
            json.put("tierNo", source.get("tierNo"));
            json.put("startValue", source.get("startValue"));
            json.put("endValue", source.get("endValue"));
            json.put("rateValue", source.get("rateValue"));
            json.put("intervalMode", source.get("intervalMode"));
            json.put("status", source.get("status"));
            json.put("remark", source.get("remark"));

            String ruleCode = stringValue(source.get("ruleCode"));
            bundle.ruleTiersByRuleCode.computeIfAbsent(ruleCode, key -> new ArrayList<>()).add(json);

            CostPublishSnapshot snapshot = new CostPublishSnapshot();
            snapshot.setSnapshotType(SNAPSHOT_RULE_TIER);
            snapshot.setObjectCode(ruleCode + "#T" + source.get("tierNo"));
            snapshot.setObjectName(ruleCode + "阶梯" + source.get("tierNo"));
            snapshot.setSnapshotJson(writeJson(json));
            snapshot.setSortNo(sortNo++);
            bundle.snapshotRows.add(snapshot);
        }
    }

    private Map<String, Object> buildSceneSnapshot(CostScene scene)
    {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("sceneCode", scene.getSceneCode());
        json.put("sceneName", scene.getSceneName());
        json.put("businessDomain", scene.getBusinessDomain());
        json.put("orgCode", scene.getOrgCode());
        json.put("sceneType", scene.getSceneType());
        json.put("status", scene.getStatus());
        json.put("remark", scene.getRemark());
        return json;
    }

    private List<Map<String, Object>> buildFeeDiffSummary(SnapshotBundle fromBundle, SnapshotBundle toBundle, String feeCode)
    {
        Set<String> feeCodes = new TreeSet<>();
        if (fromBundle != null) feeCodes.addAll(fromBundle.feesByCode.keySet());
        if (toBundle != null) feeCodes.addAll(toBundle.feesByCode.keySet());
        if (StringUtils.isNotEmpty(feeCode))
        {
            feeCodes = feeCodes.stream().filter(item -> StringUtils.equals(item, feeCode)).collect(Collectors.toCollection(TreeSet::new));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (String code : feeCodes)
        {
            Map<String, Object> fromDigest = buildFeeDigest(fromBundle, code);
            Map<String, Object> toDigest = buildFeeDigest(toBundle, code);
            String changeType = determineChangeType(fromDigest, toDigest);
            if ("UNCHANGED".equals(changeType))
            {
                continue;
            }

            Map<String, Object> fromFee = fromBundle == null ? null : fromBundle.feesByCode.get(code);
            Map<String, Object> toFee = toBundle == null ? null : toBundle.feesByCode.get(code);
            String feeName = stringValue(firstNonNull(toFee == null ? null : toFee.get("feeName"), fromFee == null ? null : fromFee.get("feeName")));
            int ruleChangeCount = countFeeRuleChanges(fromBundle, toBundle, code);
            int variableChangeCount = countFeeVariableChanges(fromBundle, toBundle, code);
            boolean feeDirectChanged = !Objects.equals(canonicalJson(fromFee), canonicalJson(toFee));

            LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            item.put("feeCode", code);
            item.put("feeName", feeName);
            item.put("changeType", changeType);
            item.put("directChanged", feeDirectChanged);
            item.put("ruleChangeCount", ruleChangeCount);
            item.put("variableChangeCount", variableChangeCount);
            item.put("changedFields", compareChangedFields(fromFee, toFee));
            item.put("summaryText", buildFeeSummaryText(changeType, feeDirectChanged, ruleChangeCount, variableChangeCount));
            item.put("fromFee", fromFee);
            item.put("toFee", toFee);
            item.put("fromRules", buildFeeRuleList(fromBundle, code));
            item.put("toRules", buildFeeRuleList(toBundle, code));
            item.put("fromVariables", buildFeeVariableList(fromBundle, code));
            item.put("toVariables", buildFeeVariableList(toBundle, code));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildRuleDiffSummary(SnapshotBundle fromBundle, SnapshotBundle toBundle, String feeCode)
    {
        Set<String> ruleCodes = new TreeSet<>();
        if (fromBundle != null) ruleCodes.addAll(fromBundle.rulesByCode.keySet());
        if (toBundle != null) ruleCodes.addAll(toBundle.rulesByCode.keySet());
        List<Map<String, Object>> result = new ArrayList<>();
        for (String ruleCode : ruleCodes)
        {
            String currentFeeCode = firstNonBlank(
                    fromBundle == null ? null : fromBundle.ruleToFeeCode.get(ruleCode),
                    toBundle == null ? null : toBundle.ruleToFeeCode.get(ruleCode));
            if (StringUtils.isNotEmpty(feeCode) && !StringUtils.equals(feeCode, currentFeeCode))
            {
                continue;
            }
            Map<String, Object> fromComposite = buildRuleComposite(fromBundle, ruleCode);
            Map<String, Object> toComposite = buildRuleComposite(toBundle, ruleCode);
            String changeType = determineChangeType(fromComposite, toComposite);
            if ("UNCHANGED".equals(changeType))
            {
                continue;
            }
            Map<String, Object> fromRule = fromBundle == null ? null : fromBundle.rulesByCode.get(ruleCode);
            Map<String, Object> toRule = toBundle == null ? null : toBundle.rulesByCode.get(ruleCode);
            LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            item.put("feeCode", currentFeeCode);
            item.put("feeName", stringValue(firstNonNull(toRule == null ? null : toRule.get("feeName"), fromRule == null ? null : fromRule.get("feeName"))));
            item.put("ruleCode", ruleCode);
            item.put("ruleName", stringValue(firstNonNull(toRule == null ? null : toRule.get("ruleName"), fromRule == null ? null : fromRule.get("ruleName"))));
            item.put("changeType", changeType);
            item.put("changedFields", compareChangedFields(fromRule, toRule));
            item.put("conditionChangeCount", countCollectionDiff(
                    fromBundle == null ? null : fromBundle.ruleConditionsByRuleCode.get(ruleCode),
                    toBundle == null ? null : toBundle.ruleConditionsByRuleCode.get(ruleCode)));
            item.put("tierChangeCount", countCollectionDiff(
                    fromBundle == null ? null : fromBundle.ruleTiersByRuleCode.get(ruleCode),
                    toBundle == null ? null : toBundle.ruleTiersByRuleCode.get(ruleCode)));
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

    private List<Map<String, Object>> buildSceneDiffSummary(Map<String, Object> fromScene, Map<String, Object> toScene)
    {
        Set<String> fields = new LinkedHashSet<>();
        if (fromScene != null) fields.addAll(fromScene.keySet());
        if (toScene != null) fields.addAll(toScene.keySet());
        List<Map<String, Object>> result = new ArrayList<>();
        for (String field : fields)
        {
            Object fromValue = fromScene == null ? null : fromScene.get(field);
            Object toValue = toScene == null ? null : toScene.get(field);
            if (Objects.equals(canonicalJsonSingle(fromValue), canonicalJsonSingle(toValue)))
            {
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

    private Map<String, Object> buildSnapshotCounts(SnapshotBundle bundle, String feeCode)
    {
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

    private Map<String, Object> buildSnapshotGroups(SnapshotBundle bundle, String feeCode)
    {
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

    private Map<String, Object> buildFeeDigest(SnapshotBundle bundle, String feeCode)
    {
        if (bundle == null || !bundle.feesByCode.containsKey(feeCode))
        {
            return null;
        }
        LinkedHashMap<String, Object> digest = new LinkedHashMap<>();
        digest.put("fee", bundle.feesByCode.get(feeCode));
        digest.put("rules", buildFeeRuleCompositeList(bundle, feeCode));
        digest.put("variables", buildFeeVariableList(bundle, feeCode));
        return digest;
    }

    private Map<String, Object> buildRuleComposite(SnapshotBundle bundle, String ruleCode)
    {
        if (bundle == null || !bundle.rulesByCode.containsKey(ruleCode))
        {
            return null;
        }
        LinkedHashMap<String, Object> composite = new LinkedHashMap<>();
        composite.put("rule", bundle.rulesByCode.get(ruleCode));
        composite.put("conditions", normalizeCollection(bundle.ruleConditionsByRuleCode.get(ruleCode)));
        composite.put("tiers", normalizeCollection(bundle.ruleTiersByRuleCode.get(ruleCode)));
        return composite;
    }

    private List<Map<String, Object>> buildFeeRuleCompositeList(SnapshotBundle bundle, String feeCode)
    {
        bundle = normalizeBundle(bundle);
        List<Map<String, Object>> result = new ArrayList<>();
        for (String ruleCode : bundle.feeRuleCodes.getOrDefault(feeCode, new TreeSet<>()))
        {
            result.add(buildRuleComposite(bundle, ruleCode));
        }
        return result;
    }

    private int countFeeRuleChanges(SnapshotBundle fromBundle, SnapshotBundle toBundle, String feeCode)
    {
        Set<String> ruleCodes = new TreeSet<>();
        if (fromBundle != null) ruleCodes.addAll(fromBundle.feeRuleCodes.getOrDefault(feeCode, new TreeSet<>()));
        if (toBundle != null) ruleCodes.addAll(toBundle.feeRuleCodes.getOrDefault(feeCode, new TreeSet<>()));
        int count = 0;
        for (String ruleCode : ruleCodes)
        {
            if (!"UNCHANGED".equals(determineChangeType(buildRuleComposite(fromBundle, ruleCode), buildRuleComposite(toBundle, ruleCode))))
            {
                count++;
            }
        }
        return count;
    }

    private int countFeeVariableChanges(SnapshotBundle fromBundle, SnapshotBundle toBundle, String feeCode)
    {
        Set<String> variableCodes = new TreeSet<>();
        if (fromBundle != null) variableCodes.addAll(fromBundle.feeReferencedVariables.getOrDefault(feeCode, new TreeSet<>()));
        if (toBundle != null) variableCodes.addAll(toBundle.feeReferencedVariables.getOrDefault(feeCode, new TreeSet<>()));
        int count = 0;
        for (String variableCode : variableCodes)
        {
            String fromJson = canonicalJson(fromBundle == null ? null : fromBundle.variablesByCode.get(variableCode));
            String toJson = canonicalJson(toBundle == null ? null : toBundle.variablesByCode.get(variableCode));
            if (!Objects.equals(fromJson, toJson))
            {
                count++;
            }
        }
        return count;
    }

    private int countCollectionDiff(List<Map<String, Object>> fromList, List<Map<String, Object>> toList)
    {
        Set<String> fromSet = normalizeCollection(fromList).stream().map(this::canonicalJson).collect(Collectors.toSet());
        Set<String> toSet = normalizeCollection(toList).stream().map(this::canonicalJson).collect(Collectors.toSet());
        Set<String> merged = new LinkedHashSet<>(fromSet);
        merged.addAll(toSet);
        int sameCount = 0;
        for (String item : merged)
        {
            if (fromSet.contains(item) && toSet.contains(item))
            {
                sameCount++;
            }
        }
        return merged.size() - sameCount;
    }

    private List<Map<String, Object>> buildFeeList(SnapshotBundle bundle, String feeCode)
    {
        bundle = normalizeBundle(bundle);
        if (StringUtils.isNotEmpty(feeCode))
        {
            return bundle.feesByCode.containsKey(feeCode) ? Collections.singletonList(bundle.feesByCode.get(feeCode)) : Collections.emptyList();
        }
        return new ArrayList<>(bundle.feesByCode.values());
    }

    private List<Map<String, Object>> buildFeeVariableList(SnapshotBundle bundle, String feeCode)
    {
        bundle = normalizeBundle(bundle);
        if (StringUtils.isEmpty(feeCode))
        {
            return new ArrayList<>(bundle.variablesByCode.values());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String variableCode : bundle.feeReferencedVariables.getOrDefault(feeCode, new TreeSet<>()))
        {
            Map<String, Object> variable = bundle.variablesByCode.get(variableCode);
            if (variable != null)
            {
                result.add(variable);
            }
        }
        return result;
    }

    private List<Map<String, Object>> buildFeeRuleList(SnapshotBundle bundle, String feeCode)
    {
        bundle = normalizeBundle(bundle);
        if (StringUtils.isEmpty(feeCode))
        {
            return new ArrayList<>(bundle.rulesByCode.values());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String ruleCode : bundle.feeRuleCodes.getOrDefault(feeCode, new TreeSet<>()))
        {
            Map<String, Object> rule = bundle.rulesByCode.get(ruleCode);
            if (rule != null)
            {
                result.add(rule);
            }
        }
        return result;
    }

    private List<Map<String, Object>> buildFeeConditionList(SnapshotBundle bundle, String feeCode)
    {
        bundle = normalizeBundle(bundle);
        Collection<String> ruleCodes = StringUtils.isEmpty(feeCode) ? bundle.rulesByCode.keySet() : bundle.feeRuleCodes.getOrDefault(feeCode, new TreeSet<>());
        List<Map<String, Object>> result = new ArrayList<>();
        for (String ruleCode : ruleCodes)
        {
            result.addAll(bundle.ruleConditionsByRuleCode.getOrDefault(ruleCode, Collections.emptyList()));
        }
        return result;
    }

    private List<Map<String, Object>> buildFeeTierList(SnapshotBundle bundle, String feeCode)
    {
        bundle = normalizeBundle(bundle);
        Collection<String> ruleCodes = StringUtils.isEmpty(feeCode) ? bundle.rulesByCode.keySet() : bundle.feeRuleCodes.getOrDefault(feeCode, new TreeSet<>());
        List<Map<String, Object>> result = new ArrayList<>();
        for (String ruleCode : ruleCodes)
        {
            result.addAll(bundle.ruleTiersByRuleCode.getOrDefault(ruleCode, Collections.emptyList()));
        }
        return result;
    }

    /**
     * 将空快照归一化为空对象，避免发布前检查、版本详情与差异视图在“无历史版本/无快照”场景下空指针。
     *
     * @param bundle 快照包
     * @return 可安全访问的快照包
     */
    private SnapshotBundle normalizeBundle(SnapshotBundle bundle)
    {
        return bundle == null ? new SnapshotBundle() : bundle;
    }

    private void sortBundleCollections(SnapshotBundle bundle)
    {
        for (List<Map<String, Object>> items : bundle.ruleConditionsByRuleCode.values())
        {
            items.sort(Comparator.comparing((Map<String, Object> item) -> stringValue(item.get("ruleCode")))
                    .thenComparing(item -> stringValue(item.get("groupNo")))
                    .thenComparing(item -> stringValue(item.get("sortNo"))));
        }
        for (List<Map<String, Object>> items : bundle.ruleTiersByRuleCode.values())
        {
            items.sort(Comparator.comparing((Map<String, Object> item) -> stringValue(item.get("ruleCode")))
                    .thenComparing(item -> stringValue(item.get("tierNo"))));
        }
    }

    private String buildNextVersionNo(Long sceneId, Date now)
    {
        String prefix = "V" + DateUtils.parseDateToStr("yyyy.MM", now) + ".";
        Long sequence = publishVersionMapper.selectMonthlyVersionSequence(sceneId, prefix);
        long next = sequence == null ? 1L : sequence + 1L;
        return prefix + VERSION_SEQ_FORMAT.format(next);
    }

    private String buildSnapshotHash(List<CostPublishSnapshot> snapshots)
    {
        List<String> pieces = snapshots.stream()
                .sorted(Comparator.comparing(CostPublishSnapshot::getSnapshotType)
                        .thenComparing(CostPublishSnapshot::getObjectCode, Comparator.nullsLast(String::compareTo)))
                .map(item -> item.getSnapshotType() + "|" + item.getObjectCode() + "|" + canonicalJson(parseJsonMap(item.getSnapshotJson())))
                .collect(Collectors.toList());
        return sha256(String.join("\n", pieces));
    }

    private CostPublishVersion requireVersion(Long versionId)
    {
        CostPublishVersion version = publishVersionMapper.selectPublishVersionDetail(versionId);
        if (StringUtils.isNull(version))
        {
            throw new ServiceException("发布版本不存在，请刷新后重试");
        }
        return version;
    }

    private CostPublishCheckItemVo createCheckItem(String level, String code, String title, String message)
    {
        CostPublishCheckItemVo item = new CostPublishCheckItemVo();
        item.setLevel(level);
        item.setCode(code);
        item.setTitle(title);
        item.setMessage(message);
        return item;
    }

    private void addRefVariable(SnapshotBundle bundle, String feeCode, String variableCode)
    {
        if (StringUtils.isEmpty(feeCode) || StringUtils.isEmpty(variableCode))
        {
            return;
        }
        bundle.feeReferencedVariables.computeIfAbsent(feeCode, key -> new TreeSet<>()).add(variableCode);
    }

    private long getLongValue(Map<String, Object> map, String key)
    {
        if (map == null || map.get(key) == null)
        {
            return 0L;
        }
        return Long.parseLong(String.valueOf(map.get(key)));
    }

    /**
     * 解析最近一次发布校验结果编码。
     *
     * @param blockingCount 阻断数量
     * @param warningCount 告警数量
     * @param latestVersion 最近版本
     * @return 校验结果
     */
    private String resolveCheckResult(long blockingCount, long warningCount, CostPublishVersion latestVersion)
    {
        if (latestVersion == null)
        {
            return "UNAVAILABLE";
        }
        if (blockingCount > 0)
        {
            return "BLOCK";
        }
        if (warningCount > 0)
        {
            return "WARN";
        }
        return "PASS";
    }

    /**
     * 解析最近一次发布校验结果文案。
     *
     * @param blockingCount 阻断数量
     * @param warningCount 告警数量
     * @param latestVersion 最近版本
     * @return 文案
     */
    private String resolveCheckLabel(long blockingCount, long warningCount, CostPublishVersion latestVersion)
    {
        if (latestVersion == null)
        {
            return "暂无校验";
        }
        if (blockingCount > 0)
        {
            return "存在阻断";
        }
        if (warningCount > 0)
        {
            return "存在告警";
        }
        return "校验通过";
    }

    private String determineChangeType(Map<String, Object> fromObject, Map<String, Object> toObject)
    {
        if (fromObject == null && toObject == null)
        {
            return "UNCHANGED";
        }
        if (fromObject == null)
        {
            return "ADDED";
        }
        if (toObject == null)
        {
            return "REMOVED";
        }
        return Objects.equals(canonicalJson(fromObject), canonicalJson(toObject)) ? "UNCHANGED" : "CHANGED";
    }

    private List<String> compareChangedFields(Map<String, Object> fromObject, Map<String, Object> toObject)
    {
        Set<String> fields = new LinkedHashSet<>();
        if (fromObject != null) fields.addAll(fromObject.keySet());
        if (toObject != null) fields.addAll(toObject.keySet());
        List<String> changed = new ArrayList<>();
        for (String field : fields)
        {
            Object fromValue = fromObject == null ? null : fromObject.get(field);
            Object toValue = toObject == null ? null : toObject.get(field);
            if (!Objects.equals(canonicalJsonSingle(fromValue), canonicalJsonSingle(toValue)))
            {
                changed.add(field);
            }
        }
        return changed;
    }

    private String buildFeeSummaryText(String changeType, boolean feeDirectChanged, int ruleChangeCount, int variableChangeCount)
    {
        if ("ADDED".equals(changeType))
        {
            return "该费用首次进入当前版本快照。";
        }
        if ("REMOVED".equals(changeType))
        {
            return "该费用未再进入当前版本快照。";
        }
        List<String> parts = new ArrayList<>();
        if (feeDirectChanged) parts.add("费用主数据有变更");
        if (ruleChangeCount > 0) parts.add(String.format(Locale.ROOT, "%d条规则发生变化", ruleChangeCount));
        if (variableChangeCount > 0) parts.add(String.format(Locale.ROOT, "%d个引用变量口径发生变化", variableChangeCount));
        return parts.isEmpty() ? "费用快照有变化。" : String.join("，", parts) + "。";
    }

    private String resolveSceneFieldLabel(String field)
    {
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

    private Map<String, Object> parseJsonMap(String json)
    {
        if (StringUtils.isEmpty(json))
        {
            return new LinkedHashMap<>();
        }
        try
        {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        }
        catch (JsonProcessingException e)
        {
            throw new ServiceException("快照 JSON 解析失败");
        }
    }

    private String writeJson(Object value)
    {
        try
        {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException e)
        {
            throw new ServiceException("快照 JSON 序列化失败");
        }
    }

    private String canonicalJson(Map<String, Object> value)
    {
        if (value == null)
        {
            return "";
        }
        return canonicalJsonSingle(value);
    }

    private String canonicalJsonSingle(Object value)
    {
        try
        {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException e)
        {
            throw new ServiceException("对象序列化失败");
        }
    }

    private List<Map<String, Object>> normalizeCollection(List<Map<String, Object>> items)
    {
        if (items == null || items.isEmpty())
        {
            return Collections.emptyList();
        }
        return items.stream().sorted(Comparator.comparing(this::canonicalJson)).collect(Collectors.toList());
    }

    private String sha256(String text)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : hash)
            {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new ServiceException("系统不支持 SHA-256 摘要算法");
        }
    }

    private Object firstNonNull(Object first, Object second)
    {
        return first != null ? first : second;
    }

    private String firstNonBlank(String first, String second)
    {
        return StringUtils.isNotEmpty(first) ? first : second;
    }

    private String buildAssetLabel(Map<String, Object> row, String nameKey, String codeKey)
    {
        return String.format("%1$s(%2$s)", stringValue(row.get(nameKey)), stringValue(row.get(codeKey)));
    }

    private String stringValue(Object value)
    {
        return value == null ? "" : String.valueOf(value);
    }

    private static class SnapshotBundle
    {
        private Long sceneId;
        private String sceneCode;
        private String sceneName;
        private String snapshotHash;
        private Map<String, Object> sceneSnapshot = new LinkedHashMap<>();
        private Map<String, Map<String, Object>> feesByCode = new LinkedHashMap<>();
        private Map<String, Map<String, Object>> variablesByCode = new LinkedHashMap<>();
        private Map<String, Map<String, Object>> formulasByCode = new LinkedHashMap<>();
        private Map<String, Map<String, Object>> rulesByCode = new LinkedHashMap<>();
        private Map<String, List<Map<String, Object>>> ruleConditionsByRuleCode = new LinkedHashMap<>();
        private Map<String, List<Map<String, Object>>> ruleTiersByRuleCode = new LinkedHashMap<>();
        private Map<String, String> ruleToFeeCode = new LinkedHashMap<>();
        private Map<String, Set<String>> feeRuleCodes = new LinkedHashMap<>();
        private Map<String, Set<String>> feeReferencedVariables = new LinkedHashMap<>();
        private List<CostPublishSnapshot> snapshotRows = new ArrayList<>();
    }
}
