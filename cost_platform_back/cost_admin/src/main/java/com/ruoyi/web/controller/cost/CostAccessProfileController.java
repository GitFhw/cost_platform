package com.ruoyi.web.controller.cost;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.cost.CostAccessProfile;
import com.ruoyi.system.domain.cost.bo.CostAccessProfileBuildBatchBo;
import com.ruoyi.system.domain.cost.bo.CostAccessProfilePreviewFetchBo;
import com.ruoyi.system.service.cost.ICostAccessProfileService;
import com.ruoyi.system.service.cost.ICostRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cost/access/profile")
public class CostAccessProfileController extends BaseController {
    @Autowired
    private ICostAccessProfileService accessProfileService;

    @Autowired
    private ICostRunService runService;

    @PreAuthorize("@ss.hasPermi('cost:access:list')")
    @GetMapping("/list")
    public TableDataInfo list(CostAccessProfile query) {
        startPage();
        List<CostAccessProfile> list = accessProfileService.selectAccessProfileList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('cost:access:list')")
    @GetMapping("/options")
    public AjaxResult options(CostAccessProfile query) {
        return success(accessProfileService.selectAccessProfileOptions(query));
    }

    @PreAuthorize("@ss.hasPermi('cost:access:query')")
    @GetMapping("/{profileId}")
    public AjaxResult getInfo(@PathVariable Long profileId) {
        return success(accessProfileService.selectAccessProfileById(profileId));
    }

    @PreAuthorize("@ss.hasPermi('cost:access:query')")
    @Log(title = "数据接入", businessType = BusinessType.OTHER)
    @PostMapping("/{profileId}/preview-fetch")
    public AjaxResult previewFetch(@PathVariable Long profileId, @RequestBody(required = false) CostAccessProfilePreviewFetchBo bo) {
        return success(runService.previewBuiltInputByProfile(profileId, bo));
    }

    @PreAuthorize("@ss.hasPermi('cost:task:execute')")
    @Log(title = "数据接入", businessType = BusinessType.INSERT)
    @PostMapping("/{profileId}/input-batch")
    public AjaxResult buildInputBatch(@PathVariable Long profileId, @Validated @RequestBody CostAccessProfileBuildBatchBo bo) {
        return success(runService.createInputBatchByProfile(profileId, bo));
    }

    @PreAuthorize("@ss.hasPermi('cost:access:add')")
    @Log(title = "数据接入", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CostAccessProfile profile) {
        if (!accessProfileService.checkProfileCodeUnique(profile)) {
            return error("新增接入方案失败，同场景下方案编码已存在");
        }
        profile.setCreateBy(getUsername());
        return toAjax(accessProfileService.insertAccessProfile(profile));
    }

    @PreAuthorize("@ss.hasPermi('cost:access:edit')")
    @Log(title = "数据接入", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CostAccessProfile profile) {
        if (!accessProfileService.checkProfileCodeUnique(profile)) {
            return error("修改接入方案失败，同场景下方案编码已存在");
        }
        profile.setUpdateBy(getUsername());
        return toAjax(accessProfileService.updateAccessProfile(profile));
    }

    @PreAuthorize("@ss.hasPermi('cost:access:remove')")
    @Log(title = "数据接入", businessType = BusinessType.DELETE)
    @DeleteMapping("/{profileIds}")
    public AjaxResult remove(@PathVariable Long[] profileIds) {
        return toAjax(accessProfileService.deleteAccessProfileByIds(profileIds));
    }
}
