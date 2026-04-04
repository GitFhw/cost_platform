package com.ruoyi.system.mapper.cost;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.system.domain.cost.CostCalcTaskDetail;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 核算任务明细 Mapper
 *
 * @author codex
 */
public interface CostCalcTaskDetailMapper extends BaseMapper<CostCalcTaskDetail>
{
    /**
     * 批量写入任务明细。
     */
    int insertBatch(@Param("list") List<CostCalcTaskDetail> list);

    /**
     * 批量回写任务明细执行结果。
     */
    int updateBatchResult(@Param("list") List<CostCalcTaskDetail> list);
}
