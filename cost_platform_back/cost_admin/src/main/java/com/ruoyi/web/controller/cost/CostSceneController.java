package com.ruoyi.web.controller.cost;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.cost.CostScene;
import com.ruoyi.system.service.cost.ICostSceneService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 场景中心控制器
 *
 * @author HwFan
 */
@RestController
@RequestMapping("/cost/scene")
public class CostSceneController extends BaseController {
    @Autowired
    private ICostSceneService sceneService;

    /**
     * 查询场景列表
     */
    @PreAuthorize("@ss.hasPermi('cost:scene:list')")
    @GetMapping("/list")
    public TableDataInfo list(CostScene scene) {
        startPage();
        List<CostScene> list = sceneService.selectSceneList(scene);
        return getDataTable(list);
    }

    /**
     * 查询场景统计卡片
     */
    @PreAuthorize("@ss.hasPermi('cost:scene:list')")
    @GetMapping("/stats")
    public AjaxResult stats(CostScene scene) {
        return success(sceneService.selectSceneStats(scene));
    }

    /**
     * 查询场景治理预检查
     */
    @PreAuthorize("@ss.hasPermi('cost:scene:list')")
    @GetMapping("/governance/{sceneId}")
    public AjaxResult governance(@PathVariable Long sceneId) {
        return success(sceneService.selectSceneGovernanceCheck(sceneId));
    }

    /**
     * 查询场景选择框
     */
    @PreAuthorize("@ss.hasPermi('cost:scene:list')")
    @GetMapping("/optionselect")
    public AjaxResult optionselect(CostScene scene) {
        return success(sceneService.selectSceneOptions(scene));
    }

    /**
     * 导出场景列表
     */
    @Log(title = "场景中心", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('cost:scene:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, CostScene scene) {
        List<CostScene> list = sceneService.selectSceneList(scene);
        ExcelUtil<CostScene> util = new ExcelUtil<CostScene>(CostScene.class);
        util.exportExcel(response, list, "场景中心");
    }

    /**
     * 查询场景详情
     */
    @PreAuthorize("@ss.hasPermi('cost:scene:query')")
    @GetMapping("/{sceneId}")
    public AjaxResult getInfo(@PathVariable Long sceneId) {
        return success(sceneService.selectSceneById(sceneId));
    }

    /**
     * 新增场景
     */
    @PreAuthorize("@ss.hasPermi('cost:scene:add')")
    @Log(title = "场景中心", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CostScene scene) {
        if (!sceneService.checkSceneCodeUnique(scene)) {
            return error("新增场景'" + scene.getSceneName() + "'失败，场景编码已存在");
        }
        return toAjax(sceneService.insertScene(scene));
    }

    /**
     * 修改场景
     */
    @PreAuthorize("@ss.hasPermi('cost:scene:edit')")
    @Log(title = "场景中心", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CostScene scene) {
        if (!sceneService.checkSceneCodeUnique(scene)) {
            return error("修改场景'" + scene.getSceneName() + "'失败，场景编码已存在");
        }
        return toAjax(sceneService.updateScene(scene));
    }

    /**
     * 删除场景
     */
    @PreAuthorize("@ss.hasPermi('cost:scene:remove')")
    @Log(title = "场景中心", businessType = BusinessType.DELETE)
    @DeleteMapping("/{sceneIds}")
    public AjaxResult remove(@PathVariable Long[] sceneIds) {
        return toAjax(sceneService.deleteSceneByIds(sceneIds));
    }
}
