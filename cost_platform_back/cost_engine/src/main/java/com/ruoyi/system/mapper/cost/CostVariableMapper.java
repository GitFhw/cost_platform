package com.ruoyi.system.mapper.cost;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.system.domain.cost.CostVariable;
import com.ruoyi.system.domain.vo.CostVariableGovernanceCheckVo;

import java.util.List;
import java.util.Map;

/**
 * 变量中心Mapper接口
 *
 * @author codex
 */
public interface CostVariableMapper extends BaseMapper<CostVariable>
{
    /**
     * 查询变量列表
     */
    List<CostVariable> selectVariableList(CostVariable variable);

    /**
     * 查询变量选择框
     */
    List<CostVariable> selectVariableOptions(CostVariable variable);

    /**
     * 查询变量统计
     */
    Map<String, Object> selectVariableStats(CostVariable variable);

    /**
     * 查询变量治理预检查
     */
    CostVariableGovernanceCheckVo selectVariableGovernanceCheck(Long variableId);

    /**
     * 查询远程来源变量数量
     */
    Long countRemoteVariableByScene(Long sceneId);
}
