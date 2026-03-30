package com.ruoyi.system.service.cost;

import com.ruoyi.system.domain.cost.CostVariable;
import com.ruoyi.system.domain.vo.CostVariableGovernanceCheckVo;

import java.util.List;
import java.util.Map;

/**
 * 变量中心服务接口
 *
 * @author codex
 */
public interface ICostVariableService
{
    /**
     * 查询变量列表
     */
    List<CostVariable> selectVariableList(CostVariable variable);

    /**
     * 查询变量详情
     */
    CostVariable selectVariableById(Long variableId);

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
     * 校验变量编码是否唯一
     */
    boolean checkVariableCodeUnique(CostVariable variable);

    /**
     * 新增变量
     */
    int insertVariable(CostVariable variable);

    /**
     * 修改变量
     */
    int updateVariable(CostVariable variable);

    /**
     * 批量删除变量
     */
    int deleteVariableByIds(Long[] variableIds);

    /**
     * 第三方接口连通性测试
     */
    Map<String, Object> testRemoteConnection(Map<String, Object> request);

    /**
     * 第三方接口数据预览
     */
    Map<String, Object> previewRemoteData(Map<String, Object> request);

    /**
     * 刷新第三方变量缓存/同步状态
     */
    Map<String, Object> refreshRemoteCache(Long sceneId);
}
