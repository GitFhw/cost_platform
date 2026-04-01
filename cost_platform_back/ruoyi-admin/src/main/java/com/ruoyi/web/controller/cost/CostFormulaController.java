package com.ruoyi.web.controller.cost;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.cost.CostFormula;
import com.ruoyi.system.domain.cost.bo.CostFormulaTestBo;
import com.ruoyi.system.service.cost.ICostFormulaService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公式实验室控制器。
 *
 * @author codex
 */
@RestController
@RequestMapping("/cost/formula")
public class CostFormulaController extends BaseController
{
    @Autowired
    private ICostFormulaService formulaService;

    /**
     * 查询公式列表。
     */
    @PreAuthorize("@ss.hasPermi('cost:formula:list')")
    @GetMapping("/list")
    public TableDataInfo list(CostFormula formula)
    {
        startPage();
        List<CostFormula> list = formulaService.selectFormulaList(formula);
        return getDataTable(list);
    }

    /**
     * 查询公式统计卡片。
     */
    @PreAuthorize("@ss.hasPermi('cost:formula:list')")
    @GetMapping("/stats")
    public AjaxResult stats(CostFormula formula)
    {
        return success(formulaService.selectFormulaStats(formula));
    }

    /**
     * 查询公式治理预检查。
     */
    @PreAuthorize("@ss.hasPermi('cost:formula:list')")
    @GetMapping("/governance/{formulaId}")
    public AjaxResult governance(@PathVariable Long formulaId)
    {
        return success(formulaService.selectFormulaGovernanceCheck(formulaId));
    }

    /**
     * 查询公式选择框。
     */
    @PreAuthorize("@ss.hasPermi('cost:formula:list')")
    @GetMapping("/optionselect")
    public AjaxResult optionselect(CostFormula formula)
    {
        return success(formulaService.selectFormulaOptions(formula));
    }

    /**
     * 导出公式列表。
     */
    @Log(title = "公式实验室", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('cost:formula:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, CostFormula formula)
    {
        List<CostFormula> list = formulaService.selectFormulaList(formula);
        ExcelUtil<CostFormula> util = new ExcelUtil<>(CostFormula.class);
        util.exportExcel(response, list, "公式实验室");
    }

    /**
     * 查询公式详情。
     */
    @PreAuthorize("@ss.hasPermi('cost:formula:query')")
    @GetMapping("/{formulaId}")
    public AjaxResult getInfo(@PathVariable Long formulaId)
    {
        return success(formulaService.selectFormulaById(formulaId));
    }

    /**
     * 新增公式。
     */
    @PreAuthorize("@ss.hasPermi('cost:formula:add')")
    @Log(title = "公式实验室", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CostFormula formula)
    {
        if (!formulaService.checkFormulaCodeUnique(formula))
        {
            return error("新增公式'" + formula.getFormulaName() + "'失败，同场景下公式编码已存在");
        }
        formula.setCreateBy(getUsername());
        return toAjax(formulaService.insertFormula(formula));
    }

    /**
     * 修改公式。
     */
    @PreAuthorize("@ss.hasPermi('cost:formula:edit')")
    @Log(title = "公式实验室", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CostFormula formula)
    {
        if (!formulaService.checkFormulaCodeUnique(formula))
        {
            return error("修改公式'" + formula.getFormulaName() + "'失败，同场景下公式编码已存在");
        }
        formula.setUpdateBy(getUsername());
        return toAjax(formulaService.updateFormula(formula));
    }

    /**
     * 删除公式。
     */
    @PreAuthorize("@ss.hasPermi('cost:formula:remove')")
    @Log(title = "公式实验室", businessType = BusinessType.DELETE)
    @DeleteMapping("/{formulaIds}")
    public AjaxResult remove(@PathVariable Long[] formulaIds)
    {
        return toAjax(formulaService.deleteFormulaByIds(formulaIds));
    }

    /**
     * 测试公式。
     */
    @PreAuthorize("@ss.hasPermi('cost:formula:test')")
    @PostMapping("/test")
    public AjaxResult test(@RequestBody CostFormulaTestBo bo)
    {
        return success(formulaService.testFormula(bo, getUsername()));
    }
}
