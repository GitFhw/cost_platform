package com.ruoyi.web.controller.cost;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.cost.CostPublishVersion;
import com.ruoyi.system.domain.cost.bo.CostPublishCreateBo;
import com.ruoyi.system.service.cost.ICostPublishService;
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
 * 发布中心控制器
 *
 * @author codex
 */
@RestController
@RequestMapping("/cost/publish")
public class CostPublishController extends BaseController
{
    @Autowired
    private ICostPublishService publishService;

    /**
     * 查询发布统计
     */
    @PreAuthorize("@ss.hasPermi('cost:publish:list')")
    @GetMapping("/stats")
    public AjaxResult stats(CostPublishVersion query)
    {
        return success(publishService.selectPublishStats(query));
    }

    /**
     * 查询版本台账
     */
    @PreAuthorize("@ss.hasPermi('cost:publish:list')")
    @GetMapping("/list")
    public TableDataInfo list(CostPublishVersion query)
    {
        startPage();
        List<CostPublishVersion> list = publishService.selectPublishVersionList(query);
        return getDataTable(list);
    }

    /**
     * 查询发布前检查结果
     */
    @PreAuthorize("@ss.hasPermi('cost:publish:query')")
    @GetMapping("/precheck/{sceneId}")
    public AjaxResult precheck(@PathVariable Long sceneId)
    {
        return success(publishService.selectPublishPrecheck(sceneId));
    }

    /**
     * 查询版本详情
     */
    @PreAuthorize("@ss.hasPermi('cost:publish:query')")
    @GetMapping("/{versionId}")
    public AjaxResult detail(@PathVariable Long versionId,
            @RequestParam(value = "feeCode", required = false) String feeCode)
    {
        return success(publishService.selectPublishVersionDetail(versionId, feeCode));
    }

    /**
     * 查询版本差异
     */
    @PreAuthorize("@ss.hasPermi('cost:publish:query')")
    @GetMapping("/diff")
    public AjaxResult diff(@RequestParam("fromVersionId") Long fromVersionId,
            @RequestParam("toVersionId") Long toVersionId,
            @RequestParam(value = "feeCode", required = false) String feeCode)
    {
        return success(publishService.selectPublishDiff(fromVersionId, toVersionId, feeCode));
    }

    /**
     * 生成发布版本
     */
    @PreAuthorize("@ss.hasPermi('cost:publish:add')")
    @Log(title = "发布中心", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult publish(@Validated @RequestBody CostPublishCreateBo bo)
    {
        return toAjax(publishService.publishScene(bo));
    }

    /**
     * 设为生效
     */
    @PreAuthorize("@ss.hasPermi('cost:publish:activate')")
    @Log(title = "发布中心", businessType = BusinessType.UPDATE)
    @PutMapping("/activate/{versionId}")
    public AjaxResult activate(@PathVariable Long versionId)
    {
        return toAjax(publishService.activateVersion(versionId));
    }

    /**
     * 回滚到历史版本
     */
    @PreAuthorize("@ss.hasPermi('cost:publish:rollback')")
    @Log(title = "发布中心", businessType = BusinessType.UPDATE)
    @PutMapping("/rollback/{versionId}")
    public AjaxResult rollback(@PathVariable Long versionId)
    {
        return toAjax(publishService.rollbackVersion(versionId));
    }
}
