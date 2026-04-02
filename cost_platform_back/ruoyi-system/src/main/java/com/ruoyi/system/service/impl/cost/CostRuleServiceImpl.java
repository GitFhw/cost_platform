package com.ruoyi.system.service.impl.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.cost.CostFeeItem;
import com.ruoyi.system.domain.cost.CostFormula;
import com.ruoyi.system.domain.cost.CostRule;
import com.ruoyi.system.domain.cost.CostRuleCondition;
import com.ruoyi.system.domain.cost.CostRuleTier;
import com.ruoyi.system.domain.cost.CostVariable;
import com.ruoyi.system.domain.cost.bo.CostRuleCopyBo;
import com.ruoyi.system.domain.cost.bo.CostRuleSaveBo;
import com.ruoyi.system.domain.vo.CostRuleGovernanceCheckVo;
import com.ruoyi.system.mapper.cost.CostFeeMapper;
import com.ruoyi.system.mapper.cost.CostFormulaMapper;
import com.ruoyi.system.mapper.cost.CostRuleConditionMapper;
import com.ruoyi.system.mapper.cost.CostRuleMapper;
import com.ruoyi.system.mapper.cost.CostRuleTierMapper;
import com.ruoyi.system.mapper.cost.CostVariableMapper;
import com.ruoyi.system.service.cost.ICostExpressionService;
import com.ruoyi.system.service.cost.ICostRuleService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 规则中心服务实现
 *
 * 线程三围绕“费用 -> 规则 -> 阶梯”的工作台维护主线展开：
 * 1. 规则归属于费用，费用又归属于场景；
 * 2. 条件值由变量元数据驱动，不允许脱离变量中心自由写死字段；
 * 3. 阶梯必须显式绑定依据变量，并在保存时做连续性、重叠和空区间校验；
 * 4. 当前阶段先打通配置维护和治理预检查，为线程四发布快照与线程五运行链预留稳定结构。
 *
 * @author codex
 */
@Service
public class CostRuleServiceImpl implements ICostRuleService
{
    private static final String STATUS_DISABLED = "1";
    private static final String STATUS_ENABLED = "0";
    private static final String RULE_TYPE_FIXED_RATE = "FIXED_RATE";
    private static final String RULE_TYPE_FIXED_AMOUNT = "FIXED_AMOUNT";
    private static final String RULE_TYPE_FORMULA = "FORMULA";
    private static final String RULE_TYPE_TIER_RATE = "TIER_RATE";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CostRuleMapper ruleMapper;

    @Autowired
    private CostRuleConditionMapper conditionMapper;

    @Autowired
    private CostRuleTierMapper tierMapper;

    @Autowired
    private CostFeeMapper feeMapper;

    @Autowired
    private CostVariableMapper variableMapper;

    @Autowired
    private CostFormulaMapper formulaMapper;

    @Autowired
    private ICostExpressionService expressionService;

    /**
     * 查询规则列表
     */
    @Override
    public List<CostRule> selectRuleList(CostRule rule)
    {
        return ruleMapper.selectRuleList(rule);
    }

    /**
     * 查询规则统计
     */
    @Override
    public Map<String, Object> selectRuleStats(CostRule rule)
    {
        Map<String, Object> stats = ruleMapper.selectRuleStats(rule);
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("ruleCount", 0);
        result.put("enabledRuleCount", 0);
        result.put("tierRuleCount", 0);
        result.put("formulaRuleCount", 0);
        if (stats == null)
        {
            return result;
        }
        for (String key : result.keySet())
        {
            Object value = stats.get(key);
            result.put(key, value == null ? 0 : value);
        }
        return result;
    }

    /**
     * 查询规则治理预检查
     */
    @Override
    public CostRuleGovernanceCheckVo selectRuleGovernanceCheck(Long ruleId)
    {
        CostRuleGovernanceCheckVo check = ruleMapper.selectRuleGovernanceCheck(ruleId);
        if (StringUtils.isNull(check))
        {
            return null;
        }
        normalizeGovernanceCount(check);
        boolean hasPublishedVersionRef = check.getPublishedVersionCount() > 0;
        boolean hasTraceRef = check.getTraceCount() > 0;
        check.setCanDelete(!hasPublishedVersionRef && !hasTraceRef);
        check.setCanDisable(!hasPublishedVersionRef && !hasTraceRef);
        check.setRemoveBlockingReason(buildGovernanceReason(check, hasPublishedVersionRef, hasTraceRef));
        check.setDisableBlockingReason(buildGovernanceReason(check, hasPublishedVersionRef, hasTraceRef));
        check.setRemoveAdvice(check.getCanDelete() ? "当前规则未进入发布或追溯链路，可直接删除。"
                : "请先发布解除引用的新版本，并确保历史追溯不再依赖当前规则后再删除。");
        check.setDisableAdvice(check.getCanDisable() ? "当前规则可停用，停用后不会再参与新的规则匹配。"
                : "当前规则已进入发布或结果追溯链路，请先替换并发布新版本后再停用。");
        return check;
    }

    /**
     * 查询规则详情
     */
    @Override
    public CostRuleSaveBo selectRuleDetail(Long ruleId)
    {
        CostRule rule = ruleMapper.selectById(ruleId);
        if (StringUtils.isNull(rule))
        {
            return null;
        }
        CostRuleSaveBo detail = new CostRuleSaveBo();
        BeanUtils.copyProperties(rule, detail);
        detail.setPricingConfig(parsePricingConfig(rule.getPricingJson()));
        detail.setConditions(ruleMapper.selectConditionsByRuleId(ruleId));
        detail.setTiers(ruleMapper.selectTiersByRuleId(ruleId));
        return detail;
    }

    /**
     * 校验规则编码唯一（同一场景内唯一）
     */
    @Override
    public boolean checkRuleCodeUnique(CostRuleSaveBo rule)
    {
        Long sceneId = resolveSceneId(rule);
        Long ruleId = rule.getRuleId() == null ? -1L : rule.getRuleId();
        Long count = ruleMapper.selectCount(Wrappers.<CostRule>lambdaQuery()
                .eq(CostRule::getSceneId, sceneId)
                .eq(CostRule::getRuleCode, rule.getRuleCode())
                .ne(ruleId.longValue() != -1L, CostRule::getRuleId, ruleId));
        return count != null && count > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    /**
     * 新增规则
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertRule(CostRuleSaveBo rule)
    {
        CostRule entity = buildAndValidateRule(rule, false);
        int rows = ruleMapper.insert(entity);
        saveChildren(entity.getRuleId(), entity.getSceneId(), rule);
        return rows;
    }

    /**
     * 修改规则
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRule(CostRuleSaveBo rule)
    {
        if (rule.getRuleId() == null)
        {
            throw new ServiceException("规则主键不能为空");
        }
        validateDisableBeforeUpdate(rule);
        CostRule entity = buildAndValidateRule(rule, true);
        int rows = ruleMapper.updateById(entity);
        replaceChildren(entity.getRuleId(), entity.getSceneId(), rule);
        return rows;
    }

    /**
     * 复制规则并调整条件值
     *
     * 复制链路直接复用规则详情快照，确保条件结构、定价配置和阶梯结构一致，
     * 仅开放新规则基础信息和条件值覆盖。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int copyRule(CostRuleCopyBo request)
    {
        CostRuleSaveBo source = selectRuleDetail(request.getSourceRuleId());
        if (StringUtils.isNull(source))
        {
            throw new ServiceException("来源规则不存在，请刷新后重试");
        }
        CostRuleSaveBo target = new CostRuleSaveBo();
        BeanUtils.copyProperties(source, target);
        target.setRuleId(null);
        target.setRuleCode(request.getRuleCode());
        target.setRuleName(request.getRuleName());
        target.setPriority(request.getPriority() == null ? source.getPriority() : request.getPriority());
        target.setSortNo(request.getSortNo() == null ? source.getSortNo() : request.getSortNo());
        target.setStatus(request.getStatus());
        target.setConditions(copyConditions(request.getConditions(), source.getConditions()));
        target.setTiers(copyTiers(source.getTiers()));
        return insertRule(target);
    }

    /**
     * 删除规则
     *
     * 删除前先执行治理预检查，避免已进入发布/追溯链路的规则被误删。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRuleByIds(Long[] ruleIds)
    {
        for (Long ruleId : ruleIds)
        {
            CostRuleGovernanceCheckVo check = selectRuleGovernanceCheck(ruleId);
            if (StringUtils.isNotNull(check) && !Boolean.TRUE.equals(check.getCanDelete()))
            {
                throw new ServiceException(String.format("%1$s不能删除：%2$s", check.getRuleCode(), check.getRemoveBlockingReason()));
            }
        }
        ruleMapper.deleteConditionsByRuleIds(ruleIds);
        ruleMapper.deleteTiersByRuleIds(ruleIds);
        return ruleMapper.deleteBatchIds(Arrays.asList(ruleIds));
    }

    /**
     * 构造规则实体并完成保存前校验
     */
    private CostRule buildAndValidateRule(CostRuleSaveBo request, boolean update)
    {
        Long sceneId = resolveSceneId(request);
        validateVariableReference(sceneId, request.getQuantityVariableCode(), "计量变量");
        validatePricing(request);
        validateConditions(sceneId, request.getConditions());
        validateTiers(request);

        CostRule entity = new CostRule();
        BeanUtils.copyProperties(request, entity);
        entity.setSceneId(sceneId);
        entity.setConditionLogic(StringUtils.isEmpty(request.getConditionLogic()) ? "AND" : request.getConditionLogic());
        entity.setPriority(request.getPriority() == null ? 0 : request.getPriority());
        entity.setPricingMode(StringUtils.isEmpty(request.getPricingMode()) ? "TYPED" : request.getPricingMode());
        entity.setSortNo(request.getSortNo() == null ? 10 : request.getSortNo());
        entity.setRuleName(StringUtils.isEmpty(request.getRuleName()) ? request.getRuleCode() : request.getRuleName());
        entity.setPricingJson(writePricingConfig(request.getPricingConfig()));
        entity.setAmountFormulaCode(StringUtils.trim(request.getAmountFormulaCode()));
        if (!update)
        {
            entity.setRuleId(null);
        }
        return entity;
    }

    /**
     * 通过费用主数据反推规则归属场景，并校验入参一致性
     */
    private Long resolveSceneId(CostRuleSaveBo request)
    {
        CostFeeItem feeItem = feeMapper.selectById(request.getFeeId());
        if (StringUtils.isNull(feeItem))
        {
            throw new ServiceException("所属费用不存在，请重新选择费用");
        }
        if (request.getSceneId() != null && !Objects.equals(request.getSceneId(), feeItem.getSceneId()))
        {
            throw new ServiceException("规则所属场景必须与费用所属场景一致");
        }
        request.setSceneId(feeItem.getSceneId());
        return feeItem.getSceneId();
    }

    /**
     * 按规则类型校验结构化定价配置
     */
    private void validatePricing(CostRuleSaveBo request)
    {
        String ruleType = request.getRuleType();
        Map<String, Object> pricingConfig = request.getPricingConfig() == null ? new LinkedHashMap<>() : request.getPricingConfig();
        if (RULE_TYPE_FIXED_RATE.equals(ruleType) && toBigDecimal(pricingConfig.get("rateValue")) == null)
        {
            throw new ServiceException("固定费率规则必须填写费率值");
        }
        if (RULE_TYPE_FIXED_AMOUNT.equals(ruleType) && toBigDecimal(pricingConfig.get("amountValue")) == null)
        {
            throw new ServiceException("固定金额规则必须填写固定金额");
        }
        if (RULE_TYPE_FORMULA.equals(ruleType))
        {
            bindAmountFormula(request);
        }
        if (RULE_TYPE_TIER_RATE.equals(ruleType))
        {
            CostVariable quantityVariable = requireQuantityVariable(request);
            if (request.getTiers() == null || request.getTiers().isEmpty())
            {
                throw new ServiceException("阶梯费率规则必须维护至少一条阶梯明细");
            }
            validateNumericVariable(quantityVariable, "阶梯依据变量");
        }
        if (RULE_TYPE_FORMULA.equals(ruleType) && StringUtils.isNotEmpty(request.getQuantityVariableCode()))
        {
            validateNumericVariable(requireQuantityVariable(request), "公式计量变量");
        }
    }

    /**
     * 绑定金额公式编码并回填标准表达式。
     */
    private void bindAmountFormula(CostRuleSaveBo request)
    {
        if (StringUtils.isNotEmpty(request.getAmountFormulaCode()))
        {
            CostFormula formula = requireEnabledAmountFormula(request.getSceneId(), request.getAmountFormulaCode());
            request.setAmountFormula(formula.getFormulaExpr());
            return;
        }
        if (StringUtils.isNotEmpty(request.getAmountFormula()))
        {
            CostFormula matchedFormula = resolveFormulaByExpression(request.getSceneId(), request.getAmountFormula());
            if (matchedFormula != null)
            {
                request.setAmountFormulaCode(matchedFormula.getFormulaCode());
                request.setAmountFormula(matchedFormula.getFormulaExpr());
                return;
            }
        }
        throw new ServiceException("公式金额规则必须引用金额公式编码；如为历史表达式，请先在公式实验室沉淀后再选择编码");
    }

    /**
     * 校验金额公式编码并返回公式资产。
     */
    private CostFormula requireEnabledAmountFormula(Long sceneId, String formulaCode)
    {
        CostFormula formula = formulaMapper.selectOne(Wrappers.<CostFormula>lambdaQuery()
                .eq(CostFormula::getSceneId, sceneId)
                .eq(CostFormula::getFormulaCode, formulaCode));
        if (formula == null)
        {
            throw new ServiceException("金额公式编码不存在，请先在公式实验室维护");
        }
        if (!STATUS_ENABLED.equals(formula.getStatus()))
        {
            throw new ServiceException("金额公式编码已停用，不能继续引用");
        }
        return formula;
    }

    /**
     * 按表达式尝试匹配既有公式资产，帮助旧规则逐步收口到公式编码。
     */
    private CostFormula resolveFormulaByExpression(Long sceneId, String formulaExpr)
    {
        if (sceneId == null || StringUtils.isEmpty(formulaExpr))
        {
            return null;
        }
        List<CostFormula> formulas = formulaMapper.selectList(Wrappers.<CostFormula>lambdaQuery()
                .eq(CostFormula::getSceneId, sceneId)
                .eq(CostFormula::getFormulaExpr, StringUtils.trim(formulaExpr))
                .eq(CostFormula::getStatus, STATUS_ENABLED)
                .last("limit 2"));
        return formulas.size() == 1 ? formulas.get(0) : null;
    }

    /**
     * 校验条件列表
     *
     * 条件编码必须来源于变量中心，避免页面自由输入导致规则口径漂移。
     */
    private void validateConditions(Long sceneId, List<CostRuleCondition> conditions)
    {
        if (conditions == null)
        {
            return;
        }
        int index = 1;
        for (CostRuleCondition condition : conditions)
        {
            if (StringUtils.isEmpty(condition.getVariableCode()))
            {
                throw new ServiceException(String.format("第%1$d条条件未选择变量", index));
            }
            validateVariableReference(sceneId, condition.getVariableCode(), String.format("第%1$d条条件变量", index));
            if (StringUtils.isEmpty(condition.getOperatorCode()))
            {
                throw new ServiceException(String.format("第%1$d条条件未选择操作符", index));
            }
            condition.setCompareValue(normalizeCompareValue(condition.getCompareValue(), condition.getOperatorCode()));
            if (requiresCompareValue(condition.getOperatorCode()) && StringUtils.isEmpty(String.valueOf(condition.getCompareValue())))
            {
                throw new ServiceException(String.format("第%1$d条条件未填写条件值", index));
            }
            condition.setSceneId(sceneId);
            condition.setGroupNo(condition.getGroupNo() == null ? 1 : condition.getGroupNo());
            condition.setSortNo(condition.getSortNo() == null ? index : condition.getSortNo());
            condition.setStatus(StringUtils.isEmpty(condition.getStatus()) ? STATUS_ENABLED : condition.getStatus());
            index++;
        }
    }

    /**
     * 校验阶梯列表
     *
     * 阶梯保存时统一做：
     * 1. 单档起止值合法性校验；
     * 2. 连续性校验，避免出现断档；
     * 3. 重叠校验，避免两个阶梯覆盖同一范围；
     * 4. 空区间校验，避免 start >= end。
     */
    private void validateTiers(CostRuleSaveBo request)
    {
        if (!RULE_TYPE_TIER_RATE.equals(request.getRuleType()))
        {
            return;
        }
        List<CostRuleTier> tiers = request.getTiers();
        if (tiers == null || tiers.isEmpty())
        {
            throw new ServiceException("阶梯规则必须配置阶梯明细");
        }
        List<CostRuleTier> normalized = new ArrayList<>(tiers);
        normalized.sort((a, b) -> Integer.compare(a.getTierNo() == null ? 0 : a.getTierNo(), b.getTierNo() == null ? 0 : b.getTierNo()));
        CostRuleTier previous = null;
        int index = 1;
        for (CostRuleTier tier : normalized)
        {
            tier.setTierNo(tier.getTierNo() == null ? index : tier.getTierNo());
            tier.setStatus(StringUtils.isEmpty(tier.getStatus()) ? STATUS_ENABLED : tier.getStatus());
            tier.setIntervalMode(StringUtils.isEmpty(tier.getIntervalMode()) ? "LEFT_CLOSED_RIGHT_OPEN" : tier.getIntervalMode());
            if (tier.getRateValue() == null)
            {
                throw new ServiceException(String.format("第%1$d档阶梯费率不能为空", index));
            }
            if (tier.getStartValue() != null && tier.getEndValue() != null && tier.getStartValue().compareTo(tier.getEndValue()) >= 0)
            {
                throw new ServiceException(String.format("第%1$d档阶梯区间无效：起始值必须小于截止值", index));
            }
            if (previous != null)
            {
                if (previous.getEndValue() == null)
                {
                    throw new ServiceException("存在无上限阶梯后仍继续维护后续档位，请检查阶梯区间");
                }
                if (tier.getStartValue() == null)
                {
                    throw new ServiceException(String.format("第%1$d档阶梯起始值不能为空", index));
                }
                int compare = previous.getEndValue().compareTo(tier.getStartValue());
                if (compare < 0)
                {
                    throw new ServiceException(String.format("第%1$d档阶梯存在断档，请确保上一档截止值与当前档起始值连续", index));
                }
                if (compare > 0)
                {
                    throw new ServiceException(String.format("第%1$d档阶梯与上一档区间重叠，请检查起始值和截止值", index));
                }
            }
            previous = tier;
            index++;
        }
    }

    /**
     * 校验变量是否存在于当前场景
     */
    private void validateVariableReference(Long sceneId, String variableCode, String fieldLabel)
    {
        if (StringUtils.isEmpty(variableCode))
        {
            return;
        }
        Long count = variableMapper.selectCount(Wrappers.<CostVariable>lambdaQuery()
                .eq(CostVariable::getSceneId, sceneId)
                .eq(CostVariable::getVariableCode, variableCode));
        if (count == null || count <= 0)
        {
            throw new ServiceException(fieldLabel + "不存在，请先在变量中心维护后再引用");
        }
    }

    /**
     * 要求当前规则已经选择计量变量，并返回变量元数据。
     */
    private CostVariable requireQuantityVariable(CostRuleSaveBo request)
    {
        if (StringUtils.isEmpty(request.getQuantityVariableCode()))
        {
            throw new ServiceException("阶梯/公式规则必须选择计量变量");
        }
        return getSceneVariable(request.getSceneId(), request.getQuantityVariableCode(), "计量变量");
    }

    /**
     * 查询场景下变量元数据。
     */
    private CostVariable getSceneVariable(Long sceneId, String variableCode, String fieldLabel)
    {
        CostVariable variable = variableMapper.selectOne(Wrappers.<CostVariable>lambdaQuery()
                .eq(CostVariable::getSceneId, sceneId)
                .eq(CostVariable::getVariableCode, variableCode)
                .last("limit 1"));
        if (StringUtils.isNull(variable))
        {
            throw new ServiceException(fieldLabel + "不存在，请先在变量中心维护后再引用");
        }
        return variable;
    }

    /**
     * 阶梯和公式的计量变量必须是数值语义变量，避免区间比较命中字符串口径。
     */
    private void validateNumericVariable(CostVariable variable, String fieldLabel)
    {
        if (StringUtils.isNull(variable))
        {
            return;
        }
        String dataType = StringUtils.isEmpty(variable.getDataType()) ? "" : variable.getDataType().toUpperCase();
        List<String> numericTypes = Arrays.asList("NUMBER", "INTEGER", "DECIMAL", "LONG", "DOUBLE");
        if (!numericTypes.contains(dataType))
        {
            throw new ServiceException(fieldLabel + "必须为数值类型变量");
        }
    }

    /**
     * 判断条件操作符是否要求填写比较值。
     */
    private boolean requiresCompareValue(String operatorCode)
    {
        return !Arrays.asList("IS_NULL", "IS_NOT_NULL").contains(StringUtils.isEmpty(operatorCode) ? "" : operatorCode.toUpperCase());
    }

    /**
     * 统一归一化条件比较值。
     *
     * 规则中心当前沿用 compareValue 字符串落库口径：
     * 1. IN / NOT_IN / BETWEEN 统一使用英文逗号分隔；
     * 2. 自动清理中英文逗号、空格和空片段；
     * 3. 其它操作符保持单值字符串语义。
     */
    private String normalizeCompareValue(String compareValue, String operatorCode)
    {
        if (compareValue == null)
        {
            return null;
        }
        String normalized = compareValue.replace('，', ',').trim();
        if (StringUtils.isEmpty(normalized))
        {
            return "";
        }
        String upperOperator = StringUtils.isEmpty(operatorCode) ? "" : operatorCode.toUpperCase();
        if (Arrays.asList("IN", "NOT_IN", "BETWEEN").contains(upperOperator))
        {
            List<String> items = new ArrayList<>();
            for (String piece : normalized.split(","))
            {
                String item = StringUtils.trim(piece);
                if (StringUtils.isNotEmpty(item))
                {
                    items.add(item);
                }
            }
            return String.join(",", items);
        }
        return normalized;
    }

    /**
     * 替换规则子表
     */
    private void replaceChildren(Long ruleId, Long sceneId, CostRuleSaveBo request)
    {
        ruleMapper.deleteConditionsByRuleIds(new Long[] { ruleId });
        ruleMapper.deleteTiersByRuleIds(new Long[] { ruleId });
        saveChildren(ruleId, sceneId, request);
    }

    /**
     * 保存规则条件和阶梯
     */
    private void saveChildren(Long ruleId, Long sceneId, CostRuleSaveBo request)
    {
        if (request.getConditions() != null)
        {
            for (CostRuleCondition condition : request.getConditions())
            {
                condition.setConditionId(null);
                condition.setRuleId(ruleId);
                condition.setSceneId(sceneId);
                conditionMapper.insert(condition);
            }
        }
        if (RULE_TYPE_TIER_RATE.equals(request.getRuleType()) && request.getTiers() != null)
        {
            for (CostRuleTier tier : request.getTiers())
            {
                tier.setTierId(null);
                tier.setRuleId(ruleId);
                tier.setSceneId(sceneId);
                tierMapper.insert(tier);
            }
        }
    }

    /**
     * 复制条件列表并覆盖比较值。
     */
    private List<CostRuleCondition> copyConditions(List<CostRuleCondition> requestConditions, List<CostRuleCondition> sourceConditions)
    {
        List<CostRuleCondition> conditions = new ArrayList<>();
        List<CostRuleCondition> baseList = requestConditions == null || requestConditions.isEmpty() ? sourceConditions : requestConditions;
        if (baseList == null)
        {
            return conditions;
        }
        for (CostRuleCondition item : baseList)
        {
            CostRuleCondition condition = new CostRuleCondition();
            BeanUtils.copyProperties(item, condition);
            condition.setConditionId(null);
            conditions.add(condition);
        }
        return conditions;
    }

    /**
     * 复制阶梯列表。
     */
    private List<CostRuleTier> copyTiers(List<CostRuleTier> sourceTiers)
    {
        List<CostRuleTier> tiers = new ArrayList<>();
        if (sourceTiers == null)
        {
            return tiers;
        }
        for (CostRuleTier item : sourceTiers)
        {
            CostRuleTier tier = new CostRuleTier();
            BeanUtils.copyProperties(item, tier);
            tier.setTierId(null);
            tiers.add(tier);
        }
        return tiers;
    }

    /**
     * 更新前停用校验
     */
    private void validateDisableBeforeUpdate(CostRuleSaveBo request)
    {
        if (request.getRuleId() == null || !STATUS_DISABLED.equals(request.getStatus()))
        {
            return;
        }
        CostRule current = ruleMapper.selectById(request.getRuleId());
        if (StringUtils.isNull(current) || STATUS_DISABLED.equals(current.getStatus()))
        {
            return;
        }
        CostRuleGovernanceCheckVo check = selectRuleGovernanceCheck(request.getRuleId());
        if (StringUtils.isNotNull(check) && !Boolean.TRUE.equals(check.getCanDisable()))
        {
            throw new ServiceException(String.format("%1$s不能停用：%2$s", check.getRuleCode(), check.getDisableBlockingReason()));
        }
    }

    /**
     * 解析结构化定价配置
     */
    private Map<String, Object> parsePricingConfig(String pricingJson)
    {
        if (StringUtils.isEmpty(pricingJson))
        {
            return new LinkedHashMap<>();
        }
        try
        {
            return objectMapper.readValue(pricingJson, new TypeReference<LinkedHashMap<String, Object>>() {});
        }
        catch (IOException e)
        {
            throw new ServiceException("规则定价配置解析失败");
        }
    }

    /**
     * 序列化结构化定价配置
     */
    private String writePricingConfig(Map<String, Object> pricingConfig)
    {
        try
        {
            return objectMapper.writeValueAsString(pricingConfig == null ? new LinkedHashMap<>() : pricingConfig);
        }
        catch (JsonProcessingException e)
        {
            throw new ServiceException("规则定价配置序列化失败");
        }
    }

    /**
     * 统一转 BigDecimal
     */
    private BigDecimal toBigDecimal(Object value)
    {
        if (value == null || StringUtils.isEmpty(String.valueOf(value)))
        {
            return null;
        }
        try
        {
            return new BigDecimal(String.valueOf(value));
        }
        catch (NumberFormatException e)
        {
            throw new ServiceException("金额/费率字段必须为数值");
        }
    }

    /**
     * 标准化治理统计
     */
    private void normalizeGovernanceCount(CostRuleGovernanceCheckVo check)
    {
        check.setConditionCount(nullSafeLong(check.getConditionCount()));
        check.setTierCount(nullSafeLong(check.getTierCount()));
        check.setPublishedVersionCount(nullSafeLong(check.getPublishedVersionCount()));
        check.setTraceCount(nullSafeLong(check.getTraceCount()));
    }

    /**
     * 构造治理阻断说明
     */
    private String buildGovernanceReason(CostRuleGovernanceCheckVo check, boolean hasPublishedVersionRef, boolean hasTraceRef)
    {
        if (!hasPublishedVersionRef && !hasTraceRef)
        {
            return "当前规则未进入发布和追溯链路";
        }
        StringJoiner joiner = new StringJoiner("；");
        if (hasPublishedVersionRef)
        {
            joiner.add(String.format("已有%1$d个发布版本快照引用当前规则", check.getPublishedVersionCount()));
        }
        if (hasTraceRef)
        {
            joiner.add(String.format("已有%1$d条追溯明细记录命中当前规则", check.getTraceCount()));
        }
        return joiner.toString();
    }

    /**
     * 空值转 0
     */
    private long nullSafeLong(Long value)
    {
        return StringUtils.isNull(value) ? 0L : value.longValue();
    }
}
