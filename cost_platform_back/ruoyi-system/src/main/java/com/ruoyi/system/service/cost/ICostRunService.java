package com.ruoyi.system.service.cost;

import com.ruoyi.system.domain.cost.CostCalcTask;
import com.ruoyi.system.domain.cost.CostResultLedger;
import com.ruoyi.system.domain.cost.CostSimulationRecord;
import com.ruoyi.system.domain.cost.bo.CostCalcTaskSubmitBo;
import com.ruoyi.system.domain.cost.bo.CostSimulationExecuteBo;

import java.util.List;
import java.util.Map;

/**
 * 线程五运行链服务接口
 *
 * @author codex
 */
public interface ICostRunService
{
    Map<String, Object> selectSimulationStats(Long sceneId);

    List<CostSimulationRecord> selectSimulationList(CostSimulationRecord query);

    Map<String, Object> executeSimulation(CostSimulationExecuteBo bo);

    Map<String, Object> selectSimulationDetail(Long simulationId);

    Map<String, Object> selectTaskStats(CostCalcTask query);

    List<CostCalcTask> selectTaskList(CostCalcTask query);

    Map<String, Object> submitTask(CostCalcTaskSubmitBo bo);

    Map<String, Object> selectTaskDetail(Long taskId);

    int retryTaskDetail(Long detailId);

    int cancelTask(Long taskId);

    Map<String, Object> selectResultStats(CostResultLedger query);

    List<CostResultLedger> selectResultList(CostResultLedger query);

    Map<String, Object> selectResultDetail(Long resultId);

    Map<String, Object> selectTraceDetail(Long traceId);

    List<Map<String, Object>> selectVersionOptions(Long sceneId);
}
