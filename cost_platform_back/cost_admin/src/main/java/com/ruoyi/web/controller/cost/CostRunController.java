package com.ruoyi.web.controller.cost;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.cost.CostCalcInputBatch;
import com.ruoyi.system.domain.cost.CostCalcTask;
import com.ruoyi.system.domain.cost.CostResultLedger;
import com.ruoyi.system.domain.cost.CostSimulationRecord;
import com.ruoyi.system.domain.cost.bo.CostCalcInputBatchCreateBo;
import com.ruoyi.system.domain.cost.bo.CostFeeCalculateBo;
import com.ruoyi.system.domain.cost.bo.CostCalcTaskSubmitBo;
import com.ruoyi.system.domain.cost.bo.CostSimulationExecuteBo;
import com.ruoyi.system.service.cost.ICostRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 线程五运行链控制器
 *
 * <p>统一承接试算、正式核算任务、结果台账和追溯解释的接口入口。
 * 当前阶段只实现线程五范围内的运行链路，不提前实现线程六的重算、审计和治理增强。</p>
 *
 * @author codex
 */
@RestController
@RequestMapping("/cost/run")
public class CostRunController extends BaseController
{
    @Autowired
    private ICostRunService runService;

    /**
     * 查询试算统计
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:list')")
    @GetMapping("/simulation/stats")
    public AjaxResult simulationStats(CostSimulationRecord query)
    {
        return success(runService.selectSimulationStats(query));
    }

    /**
     * 查询试算记录列表
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:list')")
    @GetMapping("/simulation/list")
    public TableDataInfo simulationList(CostSimulationRecord query)
    {
        startPage();
        List<CostSimulationRecord> list = runService.selectSimulationList(query);
        return getDataTable(list);
    }

    /**
     * 执行单笔试算
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:execute')")
    @Log(title = "试算中心", businessType = BusinessType.INSERT)
    @PostMapping("/simulation/execute")
    public AjaxResult simulationExecute(@Validated @RequestBody CostSimulationExecuteBo bo)
    {
        return success(runService.executeSimulation(bo));
    }

    /**
     * 执行批量试算
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:execute')")
    @Log(title = "试算中心", businessType = BusinessType.INSERT)
    @PostMapping("/simulation/batch-execute")
    public AjaxResult simulationBatchExecute(@Validated @RequestBody CostSimulationExecuteBo bo)
    {
        return success(runService.executeSimulationBatch(bo));
    }

    /**
     * 查询试算详情
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:query')")
    @GetMapping("/simulation/{simulationId}")
    public AjaxResult simulationDetail(@PathVariable Long simulationId)
    {
        return success(runService.selectSimulationDetail(simulationId));
    }

    /**
     * 查询正式核算任务统计
     */
    @PreAuthorize("@ss.hasPermi('cost:task:list')")
    @GetMapping("/task/stats")
    public AjaxResult taskStats(CostCalcTask query)
    {
        return success(runService.selectTaskStats(query));
    }

    /**
     * 查询正式核算任务总览
     */
    @PreAuthorize("@ss.hasPermi('cost:task:list')")
    @GetMapping("/task/overview")
    public AjaxResult taskOverview(CostCalcTask query)
    {
        return success(runService.selectTaskOverview(query));
    }

    /**
     * 查询正式核算任务列表
     */
    @PreAuthorize("@ss.hasPermi('cost:task:list')")
    @GetMapping("/task/list")
    public TableDataInfo taskList(CostCalcTask query)
    {
        startPage();
        List<CostCalcTask> list = runService.selectTaskList(query);
        return getDataTable(list);
    }

    /**
     * 提交正式核算任务
     */
    @PreAuthorize("@ss.hasPermi('cost:task:execute')")
    @Log(title = "正式核算", businessType = BusinessType.INSERT)
    @PostMapping("/task/submit")
    public AjaxResult submitTask(@Validated @RequestBody CostCalcTaskSubmitBo bo)
    {
        return success(runService.submitTask(bo));
    }

    /**
     * 创建正式核算导入批次
     */
    @PreAuthorize("@ss.hasPermi('cost:task:execute')")
    @Log(title = "正式核算", businessType = BusinessType.INSERT)
    @PostMapping("/task/input-batch")
    public AjaxResult createInputBatch(@Validated @RequestBody CostCalcInputBatchCreateBo bo)
    {
        return success(runService.createInputBatch(bo));
    }

    /**
     * 查询导入批次列表
     */
    @PreAuthorize("@ss.hasPermi('cost:task:list')")
    @GetMapping("/task/input-batch/list")
    public TableDataInfo inputBatchList(CostCalcInputBatch query)
    {
        startPage();
        List<CostCalcInputBatch> list = runService.selectInputBatchList(query);
        return getDataTable(list);
    }

    /**
     * 查询导入批次详情
     */
    @PreAuthorize("@ss.hasPermi('cost:task:query')")
    @GetMapping("/task/input-batch/{batchId}")
    public AjaxResult inputBatchDetail(@PathVariable Long batchId)
    {
        return success(runService.selectInputBatchDetail(batchId));
    }

    /**
     * 查询任务详情
     */
    @PreAuthorize("@ss.hasPermi('cost:task:query')")
    @GetMapping("/task/{taskId}")
    public AjaxResult taskDetail(@PathVariable Long taskId)
    {
        return success(runService.selectTaskDetail(taskId));
    }

    /**
     * 重试失败明细
     */
    @PreAuthorize("@ss.hasPermi('cost:task:retry')")
    @Log(title = "正式核算", businessType = BusinessType.UPDATE)
    @PutMapping("/task/retry/{detailId}")
    public AjaxResult retryTaskDetail(@PathVariable Long detailId)
    {
        return toAjax(runService.retryTaskDetail(detailId));
    }

    /**
     * 重试失败分片
     */
    @PreAuthorize("@ss.hasPermi('cost:task:retry')")
    @Log(title = "正式核算", businessType = BusinessType.UPDATE)
    @PutMapping("/task/partition/retry/{partitionId}")
    public AjaxResult retryTaskPartition(@PathVariable Long partitionId)
    {
        return toAjax(runService.retryTaskPartition(partitionId));
    }

    /**
     * 取消任务
     */
    @PreAuthorize("@ss.hasPermi('cost:task:cancel')")
    @Log(title = "正式核算", businessType = BusinessType.UPDATE)
    @PutMapping("/task/cancel/{taskId}")
    public AjaxResult cancelTask(@PathVariable Long taskId)
    {
        return toAjax(runService.cancelTask(taskId));
    }

    /**
     * 查询结果台账统计
     */
    @PreAuthorize("@ss.hasPermi('cost:result:list')")
    @GetMapping("/result/stats")
    public AjaxResult resultStats(CostResultLedger query)
    {
        return success(runService.selectResultStats(query));
    }

    /**
     * 查询结果台账列表
     */
    @PreAuthorize("@ss.hasPermi('cost:result:list')")
    @GetMapping("/result/list")
    public TableDataInfo resultList(CostResultLedger query)
    {
        startPage();
        List<CostResultLedger> list = runService.selectResultList(query);
        return getDataTable(list);
    }

    /**
     * 查询结果详情
     */
    @PreAuthorize("@ss.hasPermi('cost:result:query')")
    @GetMapping("/result/{resultId}")
    public AjaxResult resultDetail(@PathVariable Long resultId)
    {
        return success(runService.selectResultDetail(resultId));
    }

    /**
     * 查询追溯解释
     */
    @PreAuthorize("@ss.hasPermi('cost:result:trace')")
    @GetMapping("/trace/{traceId}")
    public AjaxResult traceDetail(@PathVariable Long traceId)
    {
        return success(runService.selectTraceDetail(traceId));
    }

    /**
     * 查询场景可用版本
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:list') or @ss.hasPermi('cost:task:list') or @ss.hasPermi('cost:result:list')")
    @GetMapping("/version-options/{sceneId}")
    public AjaxResult versionOptions(@PathVariable Long sceneId)
    {
        return success(runService.selectVersionOptions(sceneId));
    }

    /**
     * 生成运行输入模板。
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:list') or @ss.hasPermi('cost:task:list')")
    @GetMapping("/input-template")
    public AjaxResult inputTemplate(@RequestParam("sceneId") Long sceneId,
                                    @RequestParam(value = "versionId", required = false) Long versionId,
                                    @RequestParam(value = "taskType", required = false) String taskType)
    {
        return success(runService.buildInputTemplate(sceneId, versionId, taskType));
    }

    /**
     * 按费用生成运行输入模板
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:list') or @ss.hasPermi('cost:task:list')")
    @GetMapping("/input-template/fee")
    public AjaxResult feeInputTemplate(@RequestParam("sceneId") Long sceneId,
                                       @RequestParam(value = "versionId", required = false) Long versionId,
                                       @RequestParam(value = "feeId", required = false) Long feeId,
                                       @RequestParam(value = "feeCode", required = false) String feeCode,
                                       @RequestParam(value = "taskType", required = false) String taskType)
    {
        return success(runService.buildFeeInputTemplate(sceneId, versionId, feeId, feeCode, taskType));
    }

    /**
     * 按费用同步计算，便于第三方系统只针对目标费用批量取价。
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:execute')")
    @Log(title = "费用计算", businessType = BusinessType.INSERT)
    @PostMapping("/fee/calculate")
    public AjaxResult calculateFee(@Validated @RequestBody CostFeeCalculateBo bo)
    {
        return success(runService.calculateFee(bo));
    }
}
