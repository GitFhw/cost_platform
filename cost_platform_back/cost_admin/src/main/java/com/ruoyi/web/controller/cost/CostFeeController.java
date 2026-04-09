package com.ruoyi.web.controller.cost;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.cost.CostFeeItem;
import com.ruoyi.system.service.cost.ICostFeeService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 费用中心控制器
 *
 * @author HwFan
 */
@RestController
@RequestMapping("/cost/fee")
public class CostFeeController extends BaseController
{
    @Autowired
    private ICostFeeService feeService;

    /**
     * 查询费用列表
     */
    @PreAuthorize("@ss.hasPermi('cost:fee:list')")
    @GetMapping("/list")
    public TableDataInfo list(CostFeeItem feeItem)
    {
        startPage();
        List<CostFeeItem> list = feeService.selectFeeList(feeItem);
        return getDataTable(list);
    }

    /**
     * 查询费用统计卡片
     */
    @PreAuthorize("@ss.hasPermi('cost:fee:list')")
    @GetMapping("/stats")
    public AjaxResult stats(CostFeeItem feeItem)
    {
        return success(feeService.selectFeeStats(feeItem));
    }

    /**
     * 查询费用治理预检查
     */
    @PreAuthorize("@ss.hasPermi('cost:fee:list')")
    @GetMapping("/governance/{feeId}")
    public AjaxResult governance(@PathVariable Long feeId)
    {
        return success(feeService.selectFeeGovernanceCheck(feeId));
    }

    /**
     * 查询费用选择框
     */
    @PreAuthorize("@ss.hasPermi('cost:fee:list')")
    @GetMapping("/optionselect")
    public AjaxResult optionselect(CostFeeItem feeItem)
    {
        return success(feeService.selectFeeOptions(feeItem));
    }

    /**
     * 导出费用列表
     */
    @Log(title = "费用中心", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('cost:fee:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, CostFeeItem feeItem)
    {
        List<CostFeeItem> list = feeService.selectFeeList(feeItem);
        ExcelUtil<CostFeeItem> util = new ExcelUtil<>(CostFeeItem.class);
        util.exportExcel(response, list, "费用中心");
    }

    /**
     * 查询费用详情
     */
    @PreAuthorize("@ss.hasPermi('cost:fee:query')")
    @GetMapping("/{feeId}")
    public AjaxResult getInfo(@PathVariable Long feeId)
    {
        return success(feeService.selectFeeById(feeId));
    }

    /**
     * 新增费用
     */
    @PreAuthorize("@ss.hasPermi('cost:fee:add')")
    @Log(title = "费用中心", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CostFeeItem feeItem)
    {
        if (!feeService.checkFeeCodeUnique(feeItem))
        {
            return error("新增费用'" + feeItem.getFeeName() + "'失败，同场景下费用编码已存在");
        }
        return toAjax(feeService.insertFee(feeItem));
    }

    /**
     * 修改费用
     */
    @PreAuthorize("@ss.hasPermi('cost:fee:edit')")
    @Log(title = "费用中心", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CostFeeItem feeItem)
    {
        if (!feeService.checkFeeCodeUnique(feeItem))
        {
            return error("修改费用'" + feeItem.getFeeName() + "'失败，同场景下费用编码已存在");
        }
        return toAjax(feeService.updateFee(feeItem));
    }

    /**
     * 删除费用
     */
    @PreAuthorize("@ss.hasPermi('cost:fee:remove')")
    @Log(title = "费用中心", businessType = BusinessType.DELETE)
    @DeleteMapping("/{feeIds}")
    public AjaxResult remove(@PathVariable Long[] feeIds)
    {
        return toAjax(feeService.deleteFeeByIds(feeIds));
    }
}
