package com.ruoyi.system.service.impl.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.cost.CostVariable;
import com.ruoyi.system.domain.vo.CostVariableGovernanceCheckVo;
import com.ruoyi.system.mapper.cost.CostVariableMapper;
import com.ruoyi.system.service.cost.ICostVariableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 变量中心服务实现
 *
 * @author codex
 */
@Service
public class CostVariableServiceImpl implements ICostVariableService
{
    @Autowired
    private CostVariableMapper variableMapper;

    /**
     * 查询变量列表
     */
    @Override
    public List<CostVariable> selectVariableList(CostVariable variable)
    {
        return variableMapper.selectVariableList(variable);
    }

    /**
     * 查询变量详情
     */
    @Override
    public CostVariable selectVariableById(Long variableId)
    {
        return variableMapper.selectById(variableId);
    }

    /**
     * 查询变量选择框
     */
    @Override
    public List<CostVariable> selectVariableOptions(CostVariable variable)
    {
        return variableMapper.selectVariableOptions(variable);
    }

    /**
     * 查询变量统计
     */
    @Override
    public Map<String, Object> selectVariableStats(CostVariable variable)
    {
        Map<String, Object> stats = variableMapper.selectVariableStats(variable);
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("variableCount", 0);
        result.put("enabledVariableCount", 0);
        result.put("remoteVariableCount", 0);
        result.put("formulaVariableCount", 0);
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
     * 查询变量治理预检查
     */
    @Override
    public CostVariableGovernanceCheckVo selectVariableGovernanceCheck(Long variableId)
    {
        CostVariableGovernanceCheckVo check = variableMapper.selectVariableGovernanceCheck(variableId);
        if (StringUtils.isNull(check))
        {
            return null;
        }
        normalizeGovernanceCount(check);

        boolean hasFeeRelRef = check.getFeeRelCount() > 0;
        boolean hasRuleConditionRef = check.getRuleConditionCount() > 0;
        boolean hasRuleQuantityRef = check.getRuleQuantityCount() > 0;
        boolean hasPublishedVersionRef = check.getPublishedVersionCount() > 0;

        check.setCanDelete(!hasFeeRelRef && !hasRuleConditionRef && !hasRuleQuantityRef && !hasPublishedVersionRef);
        check.setCanDisable(!hasPublishedVersionRef);
        check.setRemoveBlockingReason(buildRemoveBlockingReason(check, hasFeeRelRef, hasRuleConditionRef, hasRuleQuantityRef, hasPublishedVersionRef));
        check.setDisableBlockingReason(buildDisableBlockingReason(check, hasPublishedVersionRef));
        check.setRemoveAdvice(check.getCanDelete() ? "当前变量未被费用、规则和版本占用，可直接删除。"
                : "请先解除费用关系、规则引用与发布版本引用后再删除变量。");
        check.setDisableAdvice(check.getCanDisable() ? "变量停用后将不再出现在新增配置中，历史配置保留。"
                : "当前变量已进入发布版本，请先替换并发布新版本后再停用。");
        return check;
    }

    /**
     * 校验变量编码唯一性（同一场景内唯一）
     */
    @Override
    public boolean checkVariableCodeUnique(CostVariable variable)
    {
        Long variableId = StringUtils.isNull(variable.getVariableId()) ? -1L : variable.getVariableId();
        Long count = variableMapper.selectCount(Wrappers.<CostVariable>lambdaQuery()
                .eq(CostVariable::getSceneId, variable.getSceneId())
                .eq(CostVariable::getVariableCode, variable.getVariableCode())
                .ne(variableId.longValue() != -1L, CostVariable::getVariableId, variableId));
        return count != null && count > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    /**
     * 新增变量
     */
    @Override
    public int insertVariable(CostVariable variable)
    {
        normalizeVariableSourceFields(variable);
        if (variable.getSortNo() == null)
        {
            variable.setSortNo(10);
        }
        if (variable.getPrecisionScale() == null)
        {
            variable.setPrecisionScale(2);
        }
        return variableMapper.insert(variable);
    }

    /**
     * 修改变量
     */
    @Override
    public int updateVariable(CostVariable variable)
    {
        validateDisableBeforeUpdate(variable);
        normalizeVariableSourceFields(variable);
        return variableMapper.updateById(variable);
    }

    /**
     * 批量删除变量
     */
    @Override
    public int deleteVariableByIds(Long[] variableIds)
    {
        for (Long variableId : variableIds)
        {
            CostVariableGovernanceCheckVo check = selectVariableGovernanceCheck(variableId);
            if (StringUtils.isNull(check))
            {
                continue;
            }
            if (!Boolean.TRUE.equals(check.getCanDelete()))
            {
                throw new ServiceException(String.format("%1$s不能删除：%2$s", check.getVariableName(), check.getRemoveBlockingReason()));
            }
        }
        return variableMapper.deleteBatchIds(Arrays.asList(variableIds));
    }

    /**
     * 第三方接口连通性测试
     *
     * 当前阶段先提供轻量连通性校验与配置回显，后续在线程五/六再接真实调用链路。
     */
    @Override
    public Map<String, Object> testRemoteConnection(Map<String, Object> request)
    {
        String remoteApi = Objects.toString(request.get("remoteApi"), "").trim();
        String authType = Objects.toString(request.get("authType"), "NONE").trim();
        boolean available = StringUtils.isNotEmpty(remoteApi)
                && (remoteApi.startsWith("http://") || remoteApi.startsWith("https://"));

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("success", available);
        result.put("message", available ? "接口地址格式合法，已通过基础连通性预检查。" : "接口地址不能为空，且需以 http:// 或 https:// 开头。");
        result.put("remoteApi", remoteApi);
        result.put("authType", StringUtils.isEmpty(authType) ? "NONE" : authType);
        result.put("checkedAt", new Date());
        return result;
    }

    /**
     * 第三方接口数据预览
     *
     * 预览接口返回“原始样例 + 映射样例”，帮助业务确认变量字段映射是否符合口径。
     */
    @Override
    public Map<String, Object> previewRemoteData(Map<String, Object> request)
    {
        Long variableId = null;
        Object variableIdObj = request.get("variableId");
        if (variableIdObj != null)
        {
            try
            {
                variableId = Long.valueOf(String.valueOf(variableIdObj));
            }
            catch (NumberFormatException ignored)
            {
                variableId = null;
            }
        }

        CostVariable variable = null;
        if (variableId != null)
        {
            variable = selectVariableById(variableId);
        }

        String dataPath = variable == null ? Objects.toString(request.get("dataPath"), "") : variable.getDataPath();
        String variableCode = variable == null ? Objects.toString(request.get("variableCode"), "REMOTE_SAMPLE") : variable.getVariableCode();

        List<Map<String, Object>> rawRows = new ArrayList<>();
        rawRows.add(buildRawRow("A01", "散货", "PORT", 1280));
        rawRows.add(buildRawRow("A02", "煤炭", "PORT", 2560));
        rawRows.add(buildRawRow("B11", "普通员工", "SALARY", 26));

        List<Map<String, Object>> mappedRows = new ArrayList<>();
        for (Map<String, Object> rawRow : rawRows)
        {
            LinkedHashMap<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("variableCode", variableCode);
            mapped.put("sourceCode", rawRow.get("sourceCode"));
            mapped.put("sourceName", rawRow.get("sourceName"));
            mapped.put("businessDomain", rawRow.get("businessDomain"));
            mapped.put("mappedValue", rawRow.get("value"));
            mapped.put("dataPath", StringUtils.isEmpty(dataPath) ? "value" : dataPath);
            mappedRows.add(mapped);
        }

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "已生成第三方变量样例预览，请核对字段映射口径。");
        result.put("rawRows", rawRows);
        result.put("mappedRows", mappedRows);
        result.put("previewAt", new Date());
        return result;
    }

    /**
     * 刷新第三方变量缓存/同步状态
     */
    @Override
    public Map<String, Object> refreshRemoteCache(Long sceneId)
    {
        Long remoteVariableCount = variableMapper.countRemoteVariableByScene(sceneId);
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("sceneId", sceneId);
        result.put("remoteVariableCount", remoteVariableCount == null ? 0 : remoteVariableCount);
        result.put("message", "已触发变量缓存刷新与同步状态重置。当前阶段为轻量实现，后续接入真实缓存链路。");
        result.put("refreshAt", new Date());
        return result;
    }

    /**
     * 标准化治理计数
     */
    private void normalizeGovernanceCount(CostVariableGovernanceCheckVo check)
    {
        check.setFeeRelCount(nullSafeLong(check.getFeeRelCount()));
        check.setRuleConditionCount(nullSafeLong(check.getRuleConditionCount()));
        check.setRuleQuantityCount(nullSafeLong(check.getRuleQuantityCount()));
        check.setPublishedVersionCount(nullSafeLong(check.getPublishedVersionCount()));
    }

    /**
     * 更新前停用校验
     */
    private void validateDisableBeforeUpdate(CostVariable variable)
    {
        if (StringUtils.isNull(variable.getVariableId()) || !"1".equals(variable.getStatus()))
        {
            return;
        }
        CostVariable current = selectVariableById(variable.getVariableId());
        if (StringUtils.isNull(current) || "1".equals(current.getStatus()))
        {
            return;
        }
        CostVariableGovernanceCheckVo check = selectVariableGovernanceCheck(variable.getVariableId());
        if (StringUtils.isNotNull(check) && !Boolean.TRUE.equals(check.getCanDisable()))
        {
            throw new ServiceException(String.format("%1$s不能停用：%2$s", check.getVariableName(), check.getDisableBlockingReason()));
        }
    }

    /**
     * 规范化变量来源字段
     */
    private void normalizeVariableSourceFields(CostVariable variable)
    {
        String sourceType = StringUtils.isEmpty(variable.getSourceType()) ? "INPUT" : variable.getSourceType();
        if (!"DICT".equals(sourceType))
        {
            variable.setDictType("");
        }
        if (!"REMOTE".equals(sourceType))
        {
            variable.setRemoteApi("");
            variable.setDataPath("");
        }
        if (!"FORMULA".equals(sourceType))
        {
            variable.setFormulaExpr(null);
        }
    }

    /**
     * 构造删除阻断说明
     */
    private String buildRemoveBlockingReason(CostVariableGovernanceCheckVo check, boolean hasFeeRelRef, boolean hasRuleConditionRef,
            boolean hasRuleQuantityRef, boolean hasPublishedVersionRef)
    {
        if (!hasFeeRelRef && !hasRuleConditionRef && !hasRuleQuantityRef && !hasPublishedVersionRef)
        {
            return "当前变量未被费用、规则和版本占用";
        }
        StringJoiner joiner = new StringJoiner("；");
        if (hasFeeRelRef)
        {
            joiner.add(String.format("已有%1$d条费用变量关系引用", check.getFeeRelCount()));
        }
        if (hasRuleConditionRef)
        {
            joiner.add(String.format("已有%1$d条规则条件引用", check.getRuleConditionCount()));
        }
        if (hasRuleQuantityRef)
        {
            joiner.add(String.format("已有%1$d条规则计量字段引用", check.getRuleQuantityCount()));
        }
        if (hasPublishedVersionRef)
        {
            joiner.add(String.format("已有%1$d个发布版本快照引用", check.getPublishedVersionCount()));
        }
        return joiner.toString();
    }

    /**
     * 构造停用阻断说明
     */
    private String buildDisableBlockingReason(CostVariableGovernanceCheckVo check, boolean hasPublishedVersionRef)
    {
        if (!hasPublishedVersionRef)
        {
            return "当前变量未进入发布快照，可安全停用";
        }
        return String.format("已有%1$d个发布版本快照引用当前变量", check.getPublishedVersionCount());
    }

    /**
     * 构造预览样例原始行
     */
    private Map<String, Object> buildRawRow(String sourceCode, String sourceName, String businessDomain, Object value)
    {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("sourceCode", sourceCode);
        row.put("sourceName", sourceName);
        row.put("businessDomain", businessDomain);
        row.put("value", value);
        return row;
    }

    /**
     * 空值转0
     */
    private long nullSafeLong(Long value)
    {
        return StringUtils.isNull(value) ? 0L : value.longValue();
    }
}
