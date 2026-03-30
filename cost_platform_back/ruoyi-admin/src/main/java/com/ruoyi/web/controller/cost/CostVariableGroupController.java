package com.ruoyi.web.controller.cost;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.cost.CostVariableGroup;
import com.ruoyi.system.service.cost.ICostVariableGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 变量分组控制器
 *
 * @author codex
 */
@RestController
@RequestMapping("/cost/variable/group")
public class CostVariableGroupController extends BaseController
{
    @Autowired
    private ICostVariableGroupService groupService;

    /**
     * 查询变量分组列表
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:list')")
    @GetMapping("/list")
    public TableDataInfo list(CostVariableGroup group)
    {
        startPage();
        List<CostVariableGroup> list = groupService.selectVariableGroupList(group);
        return getDataTable(list);
    }

    /**
     * 查询变量分组选择框
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:list')")
    @GetMapping("/optionselect")
    public AjaxResult optionselect(CostVariableGroup group)
    {
        return success(groupService.selectVariableGroupOptions(group));
    }

    /**
     * 查询变量分组详情
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:query')")
    @GetMapping("/{groupId}")
    public AjaxResult getInfo(@PathVariable Long groupId)
    {
        return success(groupService.selectVariableGroupById(groupId));
    }

    /**
     * 新增变量分组
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:add')")
    @Log(title = "变量分组", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CostVariableGroup group)
    {
        if (!groupService.checkGroupCodeUnique(group))
        {
            return error("新增分组'" + group.getGroupName() + "'失败，同场景下分组编码已存在");
        }
        return toAjax(groupService.insertVariableGroup(group));
    }

    /**
     * 修改变量分组
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:edit')")
    @Log(title = "变量分组", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CostVariableGroup group)
    {
        if (!groupService.checkGroupCodeUnique(group))
        {
            return error("修改分组'" + group.getGroupName() + "'失败，同场景下分组编码已存在");
        }
        return toAjax(groupService.updateVariableGroup(group));
    }

    /**
     * 删除变量分组
     */
    @PreAuthorize("@ss.hasPermi('cost:variable:remove')")
    @Log(title = "变量分组", businessType = BusinessType.DELETE)
    @DeleteMapping("/{groupIds}")
    public AjaxResult remove(@PathVariable Long[] groupIds)
    {
        return toAjax(groupService.deleteVariableGroupByIds(groupIds));
    }
}
