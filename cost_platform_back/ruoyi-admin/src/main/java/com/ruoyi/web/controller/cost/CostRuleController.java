package com.ruoyi.web.controller.cost;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.cost.CostRule;
import com.ruoyi.system.domain.cost.bo.CostRuleCopyBo;
import com.ruoyi.system.domain.cost.bo.CostRuleSaveBo;
import com.ruoyi.system.service.cost.ICostRuleService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 规则中心控制器
 *
 * @author codex
 */
@RestController
@RequestMapping("/cost/rule")
public class CostRuleController extends BaseController
{
    @Autowired
    private ICostRuleService ruleService;

    /**
     * 查询规则列表
     *
     * 页面按费用主线展开，因此列表查询默认与费用维度联动。
     */
    @PreAuthorize("@ss.hasPermi('cost:rule:list')")
    @GetMapping("/list")
    public TableDataInfo list(CostRule rule)
    {
        startPage();
        List<CostRule> list = ruleService.selectRuleList(rule);
        return getDataTable(list);
    }

    /**
     * 查询规则统计卡片
     */
    @PreAuthorize("@ss.hasPermi('cost:rule:list')")
    @GetMapping("/stats")
    public AjaxResult stats(CostRule rule)
    {
        return success(ruleService.selectRuleStats(rule));
    }

    /**
     * 查询规则治理预检查
     */
    @PreAuthorize("@ss.hasPermi('cost:rule:list')")
    @GetMapping("/governance/{ruleId}")
    public AjaxResult governance(@PathVariable Long ruleId)
    {
        return success(ruleService.selectRuleGovernanceCheck(ruleId));
    }

    /**
     * 查询规则详情
     */
    @PreAuthorize("@ss.hasPermi('cost:rule:query')")
    @GetMapping("/{ruleId}")
    public AjaxResult getInfo(@PathVariable Long ruleId)
    {
        return success(ruleService.selectRuleDetail(ruleId));
    }

    /**
     * 导出规则列表
     */
    @Log(title = "规则中心", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('cost:rule:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, CostRule rule)
    {
        List<CostRule> list = ruleService.selectRuleList(rule);
        ExcelUtil<CostRule> util = new ExcelUtil<>(CostRule.class);
        util.exportExcel(response, list, "规则中心");
    }

    /**
     * 新增规则
     */
    @PreAuthorize("@ss.hasPermi('cost:rule:add')")
    @Log(title = "规则中心", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CostRuleSaveBo rule)
    {
        if (!ruleService.checkRuleCodeUnique(rule))
        {
            return error("新增规则'" + rule.getRuleCode() + "'失败，同场景下规则编码已存在");
        }
        return toAjax(ruleService.insertRule(rule));
    }

    /**
     * 修改规则
     */
    @PreAuthorize("@ss.hasPermi('cost:rule:edit')")
    @Log(title = "规则中心", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CostRuleSaveBo rule)
    {
        if (!ruleService.checkRuleCodeUnique(rule))
        {
            return error("修改规则'" + rule.getRuleCode() + "'失败，同场景下规则编码已存在");
        }
        return toAjax(ruleService.updateRule(rule));
    }

    /**
     * 复制规则并改条件值
     */
    @PreAuthorize("@ss.hasPermi('cost:rule:add')")
    @Log(title = "规则中心", businessType = BusinessType.INSERT)
    @PostMapping("/copy")
    public AjaxResult copy(@Validated @RequestBody CostRuleCopyBo request)
    {
        CostRuleSaveBo saveBo = ruleService.selectRuleDetail(request.getSourceRuleId());
        if (saveBo == null)
        {
            return error("来源规则不存在，请刷新后重试");
        }
        saveBo.setRuleId(null);
        saveBo.setRuleCode(request.getRuleCode());
        saveBo.setRuleName(request.getRuleName());
        saveBo.setPriority(request.getPriority());
        saveBo.setSortNo(request.getSortNo());
        saveBo.setStatus(request.getStatus());
        saveBo.setConditions(request.getConditions());
        if (!ruleService.checkRuleCodeUnique(saveBo))
        {
            return error("复制规则'" + request.getRuleCode() + "'失败，同场景下规则编码已存在");
        }
        return toAjax(ruleService.copyRule(request));
    }

    /**
     * 删除规则
     */
    @PreAuthorize("@ss.hasPermi('cost:rule:remove')")
    @Log(title = "规则中心", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ruleIds}")
    public AjaxResult remove(@PathVariable Long[] ruleIds)
    {
        return toAjax(ruleService.deleteRuleByIds(ruleIds));
    }
}
