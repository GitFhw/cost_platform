package com.ruoyi.web.controller.cost;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.cost.CostVariable;
import com.ruoyi.system.service.cost.ICostVariableService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 变量中心控制器
 *
 * @author codex
 */
@RestController
@RequestMapping("/cost/variable")
public class CostVariableController extends BaseController
{
    @Autowired
    private ICostVariableService variableService;

    /**
     * 查询变量列表
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:list')")
    @GetMapping("/list")
    public TableDataInfo list(CostVariable variable)
    {
        startPage();
        List<CostVariable> list = variableService.selectVariableList(variable);
        return getDataTable(list);
    }

    /**
     * 查询变量统计卡片
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:list')")
    @GetMapping("/stats")
    public AjaxResult stats(CostVariable variable)
    {
        return success(variableService.selectVariableStats(variable));
    }

    /**
     * 查询变量治理预检查
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:list')")
    @GetMapping("/governance/{variableId}")
    public AjaxResult governance(@PathVariable Long variableId)
    {
        return success(variableService.selectVariableGovernanceCheck(variableId));
    }

    /**
     * 查询变量选择框
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:list')")
    @GetMapping("/optionselect")
    public AjaxResult optionselect(CostVariable variable)
    {
        return success(variableService.selectVariableOptions(variable));
    }

    /**
     * 导出变量列表
     */
    @Log(title = "变量中心", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('cost:variable:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, CostVariable variable)
    {
        List<CostVariable> list = variableService.selectVariableList(variable);
        ExcelUtil<CostVariable> util = new ExcelUtil<>(CostVariable.class);
        util.exportExcel(response, list, "变量中心");
    }

    /**
     * 查询变量详情
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:query')")
    @GetMapping("/{variableId}")
    public AjaxResult getInfo(@PathVariable Long variableId)
    {
        return success(variableService.selectVariableById(variableId));
    }

    /**
     * 新增变量
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:add')")
    @Log(title = "变量中心", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CostVariable variable)
    {
        if (!variableService.checkVariableCodeUnique(variable))
        {
            return error("新增变量'" + variable.getVariableName() + "'失败，同场景下变量编码已存在");
        }
        return toAjax(variableService.insertVariable(variable));
    }

    /**
     * 修改变量
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:edit')")
    @Log(title = "变量中心", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CostVariable variable)
    {
        if (!variableService.checkVariableCodeUnique(variable))
        {
            return error("修改变量'" + variable.getVariableName() + "'失败，同场景下变量编码已存在");
        }
        return toAjax(variableService.updateVariable(variable));
    }

    /**
     * 删除变量
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:remove')")
    @Log(title = "变量中心", businessType = BusinessType.DELETE)
    @DeleteMapping("/{variableIds}")
    public AjaxResult remove(@PathVariable Long[] variableIds)
    {
        return toAjax(variableService.deleteVariableByIds(variableIds));
    }

    /**
     * 测试第三方接口连通性
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:edit')")
    @PostMapping("/remote/test")
    public AjaxResult testRemote(@RequestBody Map<String, Object> request)
    {
        return success(variableService.testRemoteConnection(request));
    }

    /**
     * 预览第三方接口数据
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:list')")
    @PostMapping("/remote/preview")
    public AjaxResult previewRemote(@RequestBody Map<String, Object> request)
    {
        return success(variableService.previewRemoteData(request));
    }

    /**
     * 刷新第三方变量缓存
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:edit')")
    @PostMapping("/remote/refresh")
    public AjaxResult refreshRemote(@RequestBody(required = false) Map<String, Object> request)
    {
        Long sceneId = null;
        if (request != null && request.get("sceneId") != null)
        {
            sceneId = Long.valueOf(String.valueOf(request.get("sceneId")));
        }
        return success(variableService.refreshRemoteCache(sceneId));
    }
}
