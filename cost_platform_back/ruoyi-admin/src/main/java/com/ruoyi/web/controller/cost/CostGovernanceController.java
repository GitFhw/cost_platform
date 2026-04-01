package com.ruoyi.web.controller.cost;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.cost.CostAlarmRecord;
import com.ruoyi.system.domain.cost.CostAuditLog;
import com.ruoyi.system.domain.cost.CostBillPeriod;
import com.ruoyi.system.domain.cost.CostRecalcOrder;
import com.ruoyi.system.domain.cost.bo.CostBillPeriodSaveBo;
import com.ruoyi.system.domain.cost.bo.CostRecalcApplyBo;
import com.ruoyi.system.domain.cost.bo.CostRecalcApproveBo;
import com.ruoyi.system.service.cost.ICostGovernanceService;
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
 * 线程六治理增强控制器
 *
 * <p>统一承接账期治理、重算申请、审计台账、运行告警和运行快照缓存治理入口。</p>
 *
 * @author codex
 */
@RestController
@RequestMapping("/cost/governance")
public class CostGovernanceController extends BaseController
{
    @Autowired
    private ICostGovernanceService governanceService;

    @PreAuthorize("@ss.hasPermi('cost:period:list')")
    @GetMapping("/period/stats")
    public AjaxResult periodStats(@RequestParam(value = "sceneId", required = false) Long sceneId)
    {
        return success(governanceService.selectPeriodStats(sceneId));
    }

    @PreAuthorize("@ss.hasPermi('cost:period:list')")
    @GetMapping("/period/list")
    public TableDataInfo periodList(CostBillPeriod query)
    {
        startPage();
        List<CostBillPeriod> list = governanceService.selectPeriodList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('cost:period:query')")
    @GetMapping("/period/{periodId}")
    public AjaxResult periodDetail(@PathVariable Long periodId)
    {
        return success(governanceService.selectPeriodDetail(periodId));
    }

    @PreAuthorize("@ss.hasPermi('cost:period:add')")
    @Log(title = "账期治理", businessType = BusinessType.INSERT)
    @PostMapping("/period")
    public AjaxResult createPeriod(@Validated @RequestBody CostBillPeriodSaveBo bo)
    {
        return toAjax(governanceService.createPeriod(bo));
    }

    @PreAuthorize("@ss.hasPermi('cost:period:seal')")
    @Log(title = "账期治理", businessType = BusinessType.UPDATE)
    @PutMapping("/period/seal/{periodId}")
    public AjaxResult sealPeriod(@PathVariable Long periodId)
    {
        return toAjax(governanceService.sealPeriod(periodId));
    }

    @PreAuthorize("@ss.hasPermi('cost:period:list')")
    @GetMapping("/recalc/list")
    public TableDataInfo recalcList(CostRecalcOrder query)
    {
        startPage();
        List<CostRecalcOrder> list = governanceService.selectRecalcList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('cost:period:query')")
    @GetMapping("/recalc/{recalcId}")
    public AjaxResult recalcDetail(@PathVariable Long recalcId)
    {
        return success(governanceService.selectRecalcDetail(recalcId));
    }

    @PreAuthorize("@ss.hasPermi('cost:period:recalc')")
    @Log(title = "账期治理", businessType = BusinessType.INSERT)
    @PostMapping("/recalc/apply")
    public AjaxResult applyRecalc(@Validated @RequestBody CostRecalcApplyBo bo)
    {
        return toAjax(governanceService.applyRecalc(bo));
    }

    @PreAuthorize("@ss.hasPermi('cost:period:approve')")
    @Log(title = "账期治理", businessType = BusinessType.UPDATE)
    @PutMapping("/recalc/approve/{recalcId}")
    public AjaxResult approveRecalc(@PathVariable Long recalcId, @RequestBody CostRecalcApproveBo bo)
    {
        return toAjax(governanceService.approveRecalc(recalcId, bo));
    }

    @PreAuthorize("@ss.hasPermi('cost:period:execute')")
    @Log(title = "账期治理", businessType = BusinessType.UPDATE)
    @PutMapping("/recalc/execute/{recalcId}")
    public AjaxResult executeRecalc(@PathVariable Long recalcId)
    {
        return toAjax(governanceService.executeRecalc(recalcId));
    }

    @PreAuthorize("@ss.hasPermi('cost:audit:list')")
    @GetMapping("/audit/stats")
    public AjaxResult auditStats(CostAuditLog query)
    {
        return success(governanceService.selectAuditStats(query));
    }

    @PreAuthorize("@ss.hasPermi('cost:audit:list')")
    @GetMapping("/audit/list")
    public TableDataInfo auditList(CostAuditLog query)
    {
        startPage();
        List<CostAuditLog> list = governanceService.selectAuditList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('cost:alarm:list')")
    @GetMapping("/alarm/stats")
    public AjaxResult alarmStats(CostAlarmRecord query)
    {
        return success(governanceService.selectAlarmStats(query));
    }

    @PreAuthorize("@ss.hasPermi('cost:alarm:list')")
    @GetMapping("/alarm/list")
    public TableDataInfo alarmList(CostAlarmRecord query)
    {
        startPage();
        List<CostAlarmRecord> list = governanceService.selectAlarmList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('cost:alarm:ack')")
    @Log(title = "告警中心", businessType = BusinessType.UPDATE)
    @PutMapping("/alarm/ack/{alarmId}")
    public AjaxResult ackAlarm(@PathVariable Long alarmId)
    {
        return toAjax(governanceService.ackAlarm(alarmId));
    }

    @PreAuthorize("@ss.hasPermi('cost:alarm:resolve')")
    @Log(title = "告警中心", businessType = BusinessType.UPDATE)
    @PutMapping("/alarm/resolve/{alarmId}")
    public AjaxResult resolveAlarm(@PathVariable Long alarmId)
    {
        return toAjax(governanceService.resolveAlarm(alarmId));
    }

    @PreAuthorize("@ss.hasPermi('cost:alarm:list')")
    @GetMapping("/cache/stats")
    public AjaxResult cacheStats(@RequestParam(value = "sceneId", required = false) Long sceneId,
            @RequestParam(value = "versionId", required = false) Long versionId)
    {
        return success(governanceService.selectRuntimeCacheStats(sceneId, versionId));
    }

    @PreAuthorize("@ss.hasPermi('cost:cache:refresh')")
    @Log(title = "告警中心", businessType = BusinessType.UPDATE)
    @PutMapping("/cache/refresh")
    public AjaxResult refreshCache(@RequestParam(value = "sceneId", required = false) Long sceneId,
            @RequestParam(value = "versionId", required = false) Long versionId)
    {
        return toAjax(governanceService.refreshRuntimeCache(sceneId, versionId));
    }
}
