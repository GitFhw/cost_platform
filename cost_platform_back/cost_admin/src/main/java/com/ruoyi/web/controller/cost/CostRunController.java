package com.ruoyi.web.controller.cost;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.cost.CostCalcInputBatch;
import com.ruoyi.system.domain.cost.CostCalcTask;
import com.ruoyi.system.domain.cost.CostResultLedger;
import com.ruoyi.system.domain.cost.CostSimulationRecord;
import com.ruoyi.system.domain.cost.bo.*;
import com.ruoyi.system.service.cost.ICostRunService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 线程五运行链控制器
 *
 * <p>统一承接试算、正式核算任务、结果台账和追溯解释的接口入口。
 * 当前阶段只实现线程五范围内的运行链路，不提前实现线程六的重算、审计和治理增强。</p>
 *
 * @author HwFan
 */
@RestController
@RequestMapping("/cost/run")
public class CostRunController extends BaseController {
    @Autowired
    private ICostRunService runService;

    /**
     * 查询试算统计
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:list')")
    @GetMapping("/simulation/stats")
    public AjaxResult simulationStats(CostSimulationRecord query) {
        return success(runService.selectSimulationStats(query));
    }

    /**
     * 查询试算记录列表
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:list')")
    @GetMapping("/simulation/list")
    public TableDataInfo simulationList(CostSimulationRecord query) {
        startPage();
        List<CostSimulationRecord> list = runService.selectSimulationList(query);
        return getDataTable(list);
    }

    /**
     * 执行单笔试算
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:list') or @ss.hasPermi('cost:task:list')")
    @Log(title = "数据接入", businessType = BusinessType.OTHER)
    @PostMapping("/input-build/preview")
    public AjaxResult previewInputBuild(@Validated @RequestBody CostInputBuildPreviewBo bo) {
        return success(runService.previewBuiltInput(bo));
    }

    @PreAuthorize("@ss.hasPermi('cost:simulation:execute')")
    @Log(title = "试算中心", businessType = BusinessType.INSERT)
    @PostMapping("/simulation/execute")
    public AjaxResult simulationExecute(@Validated @RequestBody CostSimulationExecuteBo bo) {
        return success(runService.executeSimulation(bo));
    }

    /**
     * 执行批量试算
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:execute')")
    @Log(title = "试算中心", businessType = BusinessType.INSERT)
    @PostMapping("/simulation/batch-execute")
    public AjaxResult simulationBatchExecute(@Validated @RequestBody CostSimulationExecuteBo bo) {
        return success(runService.executeSimulationBatch(bo));
    }

    /**
     * 查询试算详情
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:query')")
    @GetMapping("/simulation/{simulationId}")
    public AjaxResult simulationDetail(@PathVariable Long simulationId) {
        return success(runService.selectSimulationDetail(simulationId));
    }

    /**
     * 导出试算计费明细
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:query')")
    @Log(title = "试算中心", businessType = BusinessType.EXPORT)
    @PostMapping("/simulation/charge-export")
    public void exportSimulationCharge(HttpServletResponse response, Long simulationId) {
        List<CostSimulationChargeExportRow> list = runService.selectSimulationChargeExportRows(simulationId);
        ExcelUtil<CostSimulationChargeExportRow> util = new ExcelUtil<>(CostSimulationChargeExportRow.class);
        util.exportExcel(response, list, "试算计费明细");
    }

    /**
     * 导出批量试算回归摘要
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:list')")
    @Log(title = "试算中心", businessType = BusinessType.EXPORT)
    @PostMapping("/simulation/batch-export")
    public void exportSimulationBatch(HttpServletResponse response, String simulationIds, Boolean failedOnly) {
        List<CostSimulationBatchExportRow> list = runService.selectSimulationBatchExportRows(simulationIds, failedOnly);
        ExcelUtil<CostSimulationBatchExportRow> util = new ExcelUtil<>(CostSimulationBatchExportRow.class);
        util.exportExcel(response, list, Boolean.TRUE.equals(failedOnly) ? "批量失败清单" : "批量回归摘要");
    }

    /**
     * 查询正式核算任务统计
     */
    @PreAuthorize("@ss.hasPermi('cost:task:list')")
    @GetMapping("/task/stats")
    public AjaxResult taskStats(CostCalcTask query) {
        return success(runService.selectTaskStats(query));
    }

    /**
     * 查询正式核算任务总览
     */
    @PreAuthorize("@ss.hasPermi('cost:task:list')")
    @GetMapping("/task/overview")
    public AjaxResult taskOverview(CostCalcTask query) {
        return success(runService.selectTaskOverview(query));
    }

    /**
     * 查询正式核算任务列表
     */
    @PreAuthorize("@ss.hasPermi('cost:task:list')")
    @GetMapping("/task/list")
    public TableDataInfo taskList(CostCalcTask query) {
        startPage();
        List<CostCalcTask> list = runService.selectTaskList(query);
        return getDataTable(list);
    }

    /**
     * 提交正式核算任务
     */
    @PreAuthorize("@ss.hasPermi('cost:task:execute')")
    @Log(title = "正式核算", businessType = BusinessType.INSERT)
    @PostMapping("/task/precheck")
    public AjaxResult precheckTask(@RequestBody CostCalcTaskSubmitBo bo) {
        return success(runService.precheckTask(bo));
    }

    @PreAuthorize("@ss.hasPermi('cost:task:execute')")
    @Log(title = "正式核算", businessType = BusinessType.INSERT)
    @PostMapping("/task/submit")
    public AjaxResult submitTask(@Validated @RequestBody CostCalcTaskSubmitBo bo) {
        return success(runService.submitTask(bo));
    }

    /**
     * 创建正式核算导入批次
     */
    @PreAuthorize("@ss.hasPermi('cost:task:execute')")
    @Log(title = "正式核算", businessType = BusinessType.INSERT)
    @PostMapping("/task/input-batch")
    public AjaxResult createInputBatch(@Validated @RequestBody CostCalcInputBatchCreateBo bo) {
        return success(runService.createInputBatch(bo));
    }

    /**
     * 查询导入批次列表
     */
    @PreAuthorize("@ss.hasPermi('cost:task:list')")
    @GetMapping("/task/input-batch/list")
    public TableDataInfo inputBatchList(CostCalcInputBatch query) {
        startPage();
        List<CostCalcInputBatch> list = runService.selectInputBatchList(query);
        return getDataTable(list);
    }

    /**
     * 查询导入批次详情
     */
    @PreAuthorize("@ss.hasPermi('cost:task:query')")
    @GetMapping("/task/input-batch/{batchId}")
    public AjaxResult inputBatchDetail(@PathVariable Long batchId,
                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return success(runService.selectInputBatchDetail(batchId, pageNum, pageSize));
    }

    /**
     * 查询任务详情
     */
    @PreAuthorize("@ss.hasPermi('cost:task:query')")
    @GetMapping("/task/{taskId}")
    public AjaxResult taskDetail(@PathVariable Long taskId,
                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "20") Integer pageSize) {
        return success(runService.selectTaskDetail(taskId, pageNum, pageSize));
    }

    /**
     * 重试失败明细
     */
    @PreAuthorize("@ss.hasPermi('cost:task:retry')")
    @Log(title = "正式核算", businessType = BusinessType.UPDATE)
    @PutMapping("/task/retry/{detailId}")
    public AjaxResult retryTaskDetail(@PathVariable Long detailId) {
        return toAjax(runService.retryTaskDetail(detailId));
    }

    /**
     * 重试失败分片
     */
    @PreAuthorize("@ss.hasPermi('cost:task:retry')")
    @Log(title = "正式核算", businessType = BusinessType.UPDATE)
    @PutMapping("/task/partition/retry/{partitionId}")
    public AjaxResult retryTaskPartition(@PathVariable Long partitionId) {
        return toAjax(runService.retryTaskPartition(partitionId));
    }

    /**
     * 取消任务
     */
    @PreAuthorize("@ss.hasPermi('cost:task:cancel')")
    @Log(title = "正式核算", businessType = BusinessType.UPDATE)
    @PutMapping("/task/cancel/{taskId}")
    public AjaxResult cancelTask(@PathVariable Long taskId) {
        return toAjax(runService.cancelTask(taskId));
    }

    /**
     * 查询结果台账统计
     */
    @PreAuthorize("@ss.hasPermi('cost:result:list')")
    @GetMapping("/result/stats")
    public AjaxResult resultStats(CostResultLedger query) {
        return success(runService.selectResultStats(query));
    }

    /**
     * 查询结果台账列表
     */
    @PreAuthorize("@ss.hasPermi('cost:result:list')")
    @GetMapping("/result/list")
    public TableDataInfo resultList(CostResultLedger query) {
        startPage();
        List<CostResultLedger> list = runService.selectResultList(query);
        return getDataTable(list);
    }

    /**
     * 导出结果台账
     */
    @PreAuthorize("@ss.hasPermi('cost:result:list')")
    @PostMapping("/result/export")
    public void exportResult(HttpServletResponse response, CostResultLedger query) {
        List<CostResultLedger> list = runService.selectResultList(query);
        ExcelUtil<CostResultLedger> util = new ExcelUtil<>(CostResultLedger.class);
        util.exportExcel(response, list, "结果台账");
    }

    /**
     * 查询结果详情
     */
    @PreAuthorize("@ss.hasPermi('cost:result:query')")
    @GetMapping("/result/{resultId}")
    public AjaxResult resultDetail(@PathVariable Long resultId) {
        return success(runService.selectResultDetail(resultId));
    }

    /**
     * 查询追溯解释
     */
    @PreAuthorize("@ss.hasPermi('cost:result:trace')")
    @GetMapping("/trace/{traceId}")
    public AjaxResult traceDetail(@PathVariable Long traceId) {
        return success(runService.selectTraceDetail(traceId));
    }

    /**
     * 查询场景可用版本
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:list') or @ss.hasPermi('cost:task:list') or @ss.hasPermi('cost:result:list')")
    @GetMapping("/version-options/{sceneId}")
    public AjaxResult versionOptions(@PathVariable Long sceneId) {
        return success(runService.selectVersionOptions(sceneId));
    }

    /**
     * 生成运行输入模板。
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:list') or @ss.hasPermi('cost:task:list')")
    @GetMapping("/input-template")
    public AjaxResult inputTemplate(@RequestParam("sceneId") Long sceneId,
                                    @RequestParam(value = "versionId", required = false) Long versionId,
                                    @RequestParam(value = "taskType", required = false) String taskType) {
        return success(runService.buildInputTemplate(sceneId, versionId, taskType));
    }

    /**
     * 按费用生成运行输入模板
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:list') or @ss.hasPermi('cost:task:list')")
    @GetMapping("/input-template/fee")
    public AjaxResult feeInputTemplate(@RequestParam("sceneId") Long sceneId,
                                       @RequestParam(value = "versionId", required = false) Long versionId,
                                       @RequestParam(value = "feeIds", required = false) String feeIds,
                                       @RequestParam(value = "feeId", required = false) Long feeId,
                                       @RequestParam(value = "feeCode", required = false) String feeCode,
                                       @RequestParam(value = "taskType", required = false) String taskType,
                                       @RequestParam(value = "snapshotMode", required = false) String snapshotMode) {
        return success(runService.buildFeeInputTemplate(sceneId, versionId, parseLongIdList(feeIds), feeId, feeCode,
            taskType, snapshotMode));
    }

    /**
     * 按费用同步计算，便于第三方系统只针对目标费用批量取价。
     */
    @PreAuthorize("@ss.hasPermi('cost:simulation:execute')")
    @Log(title = "费用计算", businessType = BusinessType.INSERT)
    @PostMapping("/fee/calculate")
    public AjaxResult calculateFee(@Validated @RequestBody CostFeeCalculateBo bo) {
        return success(runService.calculateFee(bo));
    }

    private List<Long> parseLongIdList(String ids) {
        if (StringUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        for (String item : ids.split(",")) {
            String value = StringUtils.trim(item);
            if (StringUtils.isNotEmpty(value)) {
                result.add(Long.valueOf(value));
            }
        }
        return result;
    }
}
