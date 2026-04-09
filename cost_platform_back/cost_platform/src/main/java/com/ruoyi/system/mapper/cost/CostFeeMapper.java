package com.ruoyi.system.mapper.cost;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.system.domain.cost.CostFeeItem;
import com.ruoyi.system.domain.vo.CostFeeGovernanceCheckVo;

import java.util.List;
import java.util.Map;

/**
 * 费用中心Mapper接口
 *
 * @author HwFan
 */
public interface CostFeeMapper extends BaseMapper<CostFeeItem> {
    /**
     * 查询费用列表
     *
     * @param feeItem 查询对象
     *
     * @return 费用集合
     */
    List<CostFeeItem> selectFeeList(CostFeeItem feeItem);

    /**
     * 查询费用选择框
     *
     * @param feeItem 查询对象
     *
     * @return 费用集合
     */
    List<CostFeeItem> selectFeeOptions(CostFeeItem feeItem);

    /**
     * 查询费用统计
     *
     * @param feeItem 查询对象
     *
     * @return 统计结果
     */
    Map<String, Object> selectFeeStats(CostFeeItem feeItem);

    /**
     * 查询费用治理预检查结果
     *
     * @param feeId 费用主键
     *
     * @return 结果
     */
    CostFeeGovernanceCheckVo selectFeeGovernanceCheck(Long feeId);
}
