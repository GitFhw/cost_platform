package com.ruoyi.system.service.cost;

import com.ruoyi.system.domain.cost.CostCalcTask;
import com.ruoyi.system.domain.cost.CostCalcInputBatch;
import com.ruoyi.system.domain.cost.CostResultLedger;
import com.ruoyi.system.domain.cost.CostSimulationRecord;
import com.ruoyi.system.domain.cost.bo.CostCalcInputBatchCreateBo;
import com.ruoyi.system.domain.cost.bo.CostFeeCalculateBo;
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
    Map<String, Object> selectSimulationStats(CostSimulationRecord query);

    List<CostSimulationRecord> selectSimulationList(CostSimulationRecord query);

    Map<String, Object> executeSimulation(CostSimulationExecuteBo bo);

    Map<String, Object> executeSimulationBatch(CostSimulationExecuteBo bo);

    Map<String, Object> selectSimulationDetail(Long simulationId);

    Map<String, Object> selectTaskStats(CostCalcTask query);

    Map<String, Object> selectTaskOverview(CostCalcTask query);

    List<CostCalcTask> selectTaskList(CostCalcTask query);

    Map<String, Object> submitTask(CostCalcTaskSubmitBo bo);

    Map<String, Object> createInputBatch(CostCalcInputBatchCreateBo bo);

    List<CostCalcInputBatch> selectInputBatchList(CostCalcInputBatch query);

    Map<String, Object> selectInputBatchDetail(Long batchId);

    Map<String, Object> selectTaskDetail(Long taskId);

    int retryTaskDetail(Long detailId);

    int retryTaskPartition(Long partitionId);

    int cancelTask(Long taskId);

    Map<String, Object> selectResultStats(CostResultLedger query);

    List<CostResultLedger> selectResultList(CostResultLedger query);

    Map<String, Object> selectResultDetail(Long resultId);

    Map<String, Object> selectTraceDetail(Long traceId);

    List<Map<String, Object>> selectVersionOptions(Long sceneId);

    Map<String, Object> buildInputTemplate(Long sceneId, Long versionId, String taskType);

    Map<String, Object> buildFeeInputTemplate(Long sceneId, Long versionId, Long feeId, String feeCode, String taskType);

    Map<String, Object> calculateFee(CostFeeCalculateBo bo);
}
