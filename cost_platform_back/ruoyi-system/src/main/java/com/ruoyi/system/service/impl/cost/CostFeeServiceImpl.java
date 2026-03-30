package com.ruoyi.system.service.impl.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.cost.CostFeeItem;
import com.ruoyi.system.domain.vo.CostFeeGovernanceCheckVo;
import com.ruoyi.system.mapper.cost.CostFeeMapper;
import com.ruoyi.system.service.cost.ICostFeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 费用中心服务实现
 *
 * @author codex
 */
@Service
public class CostFeeServiceImpl implements ICostFeeService
{
    @Autowired
    private CostFeeMapper feeMapper;

    /**
     * 查询费用列表
     */
    @Override
    public List<CostFeeItem> selectFeeList(CostFeeItem feeItem)
    {
        return feeMapper.selectFeeList(feeItem);
    }

    /**
     * 查询费用详情
     */
    @Override
    public CostFeeItem selectFeeById(Long feeId)
    {
        return feeMapper.selectById(feeId);
    }

    /**
     * 查询费用选择框
     */
    @Override
    public List<CostFeeItem> selectFeeOptions(CostFeeItem feeItem)
    {
        return feeMapper.selectFeeOptions(feeItem);
    }

    /**
     * 查询费用统计
     */
    @Override
    public Map<String, Object> selectFeeStats(CostFeeItem feeItem)
    {
        Map<String, Object> stats = feeMapper.selectFeeStats(feeItem);
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("feeCount", 0);
        result.put("enabledFeeCount", 0);
        result.put("sceneCoverageCount", 0);
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
     * 查询费用治理预检查
     */
    @Override
    public CostFeeGovernanceCheckVo selectFeeGovernanceCheck(Long feeId)
    {
        CostFeeGovernanceCheckVo check = feeMapper.selectFeeGovernanceCheck(feeId);
        if (StringUtils.isNull(check))
        {
            return null;
        }
        normalizeGovernanceCount(check);

        boolean hasRuleRef = check.getRuleCount() > 0;
        boolean hasPublishedVersionRef = check.getPublishedVersionCount() > 0;
        boolean hasResultRef = check.getResultLedgerCount() > 0;

        check.setCanDelete(!hasRuleRef && !hasPublishedVersionRef && !hasResultRef);
        check.setCanDisable(!hasPublishedVersionRef && !hasResultRef);
        check.setRemoveBlockingReason(buildRemoveBlockingReason(check, hasRuleRef, hasPublishedVersionRef, hasResultRef));
        check.setDisableBlockingReason(buildDisableBlockingReason(check, hasPublishedVersionRef, hasResultRef));
        check.setRemoveAdvice(check.getCanDelete() ? "当前费用未被规则、版本或结果占用，可直接删除。"
                : "请先解除规则依赖、发布版本引用和结果台账影响，再删除费用。");
        check.setDisableAdvice(check.getCanDisable() ? buildDisableAdvice(check)
                : "请先处理发布版本或结果台账引用，再执行停用。");
        return check;
    }

    /**
     * 校验费用编码是否唯一（同一场景内唯一）
     */
    @Override
    public boolean checkFeeCodeUnique(CostFeeItem feeItem)
    {
        Long feeId = StringUtils.isNull(feeItem.getFeeId()) ? -1L : feeItem.getFeeId();
        Long count = feeMapper.selectCount(Wrappers.<CostFeeItem>lambdaQuery()
                .eq(CostFeeItem::getSceneId, feeItem.getSceneId())
                .eq(CostFeeItem::getFeeCode, feeItem.getFeeCode())
                .ne(feeId.longValue() != -1L, CostFeeItem::getFeeId, feeId));
        return count != null && count > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    /**
     * 新增费用
     */
    @Override
    public int insertFee(CostFeeItem feeItem)
    {
        if (feeItem.getSortNo() == null)
        {
            feeItem.setSortNo(10);
        }
        return feeMapper.insert(feeItem);
    }

    /**
     * 修改费用
     */
    @Override
    public int updateFee(CostFeeItem feeItem)
    {
        validateDisableBeforeUpdate(feeItem);
        return feeMapper.updateById(feeItem);
    }

    /**
     * 批量删除费用
     *
     * 删除前先执行治理预检查，避免被规则、版本或结果链路引用时误删。
     */
    @Override
    public int deleteFeeByIds(Long[] feeIds)
    {
        for (Long feeId : feeIds)
        {
            CostFeeGovernanceCheckVo check = selectFeeGovernanceCheck(feeId);
            if (StringUtils.isNull(check))
            {
                continue;
            }
            if (!Boolean.TRUE.equals(check.getCanDelete()))
            {
                throw new ServiceException(String.format("%1$s不能删除：%2$s", check.getFeeName(), check.getRemoveBlockingReason()));
            }
        }
        return feeMapper.deleteBatchIds(Arrays.asList(feeIds));
    }

    /**
     * 标准化治理计数
     */
    private void normalizeGovernanceCount(CostFeeGovernanceCheckVo check)
    {
        check.setRuleCount(nullSafeLong(check.getRuleCount()));
        check.setVariableRelCount(nullSafeLong(check.getVariableRelCount()));
        check.setPublishedVersionCount(nullSafeLong(check.getPublishedVersionCount()));
        check.setResultLedgerCount(nullSafeLong(check.getResultLedgerCount()));
    }

    /**
     * 更新前停用校验
     */
    private void validateDisableBeforeUpdate(CostFeeItem feeItem)
    {
        if (StringUtils.isNull(feeItem.getFeeId()) || !"1".equals(feeItem.getStatus()))
        {
            return;
        }
        CostFeeItem current = selectFeeById(feeItem.getFeeId());
        if (StringUtils.isNull(current) || "1".equals(current.getStatus()))
        {
            return;
        }
        CostFeeGovernanceCheckVo check = selectFeeGovernanceCheck(feeItem.getFeeId());
        if (StringUtils.isNotNull(check) && !Boolean.TRUE.equals(check.getCanDisable()))
        {
            throw new ServiceException(String.format("%1$s不能停用：%2$s", check.getFeeName(), check.getDisableBlockingReason()));
        }
    }

    /**
     * 构造删除阻断说明
     */
    private String buildRemoveBlockingReason(CostFeeGovernanceCheckVo check, boolean hasRuleRef, boolean hasPublishedVersionRef,
            boolean hasResultRef)
    {
        if (!hasRuleRef && !hasPublishedVersionRef && !hasResultRef)
        {
            return "当前费用未被规则、版本或结果占用";
        }
        StringJoiner joiner = new StringJoiner("；");
        if (hasRuleRef)
        {
            joiner.add(String.format("已有%1$d条规则引用当前费用", check.getRuleCount()));
        }
        if (hasPublishedVersionRef)
        {
            joiner.add(String.format("已有%1$d个发布版本快照引用当前费用", check.getPublishedVersionCount()));
        }
        if (hasResultRef)
        {
            joiner.add(String.format("已有%1$d条结果台账记录绑定当前费用", check.getResultLedgerCount()));
        }
        return joiner.toString();
    }

    /**
     * 构造停用阻断说明
     */
    private String buildDisableBlockingReason(CostFeeGovernanceCheckVo check, boolean hasPublishedVersionRef, boolean hasResultRef)
    {
        if (!hasPublishedVersionRef && !hasResultRef)
        {
            return "当前费用未进入发布/结果链路，可安全停用";
        }
        StringJoiner joiner = new StringJoiner("；");
        if (hasPublishedVersionRef)
        {
            joiner.add(String.format("已有%1$d个发布版本快照引用当前费用", check.getPublishedVersionCount()));
        }
        if (hasResultRef)
        {
            joiner.add(String.format("已有%1$d条结果台账记录绑定当前费用", check.getResultLedgerCount()));
        }
        return joiner.toString();
    }

    /**
     * 构造停用建议
     */
    private String buildDisableAdvice(CostFeeGovernanceCheckVo check)
    {
        if (check.getRuleCount() <= 0)
        {
            return "当前费用无规则挂载，停用后将从维护和计费选择范围中移除。";
        }
        return String.format("当前费用仍挂载%1$d条规则，停用后将不再参与后续配置与计算选择。", check.getRuleCount());
    }

    /**
     * 空值转0
     */
    private long nullSafeLong(Long value)
    {
        return StringUtils.isNull(value) ? 0L : value.longValue();
    }
}
