package com.ruoyi.web.controller.cost;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.cost.CostOpenApp;
import com.ruoyi.system.domain.cost.CostScene;
import com.ruoyi.system.service.cost.ICostOpenAppService;
import com.ruoyi.system.service.cost.ICostSceneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 开放应用维护控制器
 */
@RestController
@RequestMapping("/cost/openApp")
public class CostOpenAppController extends BaseController {
    @Autowired
    private ICostOpenAppService openAppService;

    @Autowired
    private ICostSceneService sceneService;

    /**
     * 查询开放应用列表。
     */
    @PreAuthorize("@ss.hasPermi('cost:openApp:list')")
    @GetMapping("/list")
    public TableDataInfo list(CostOpenApp query) {
        startPage();
        List<CostOpenApp> list = openAppService.selectOpenAppList(query);
        return getDataTable(list);
    }

    /**
     * 查询开放应用详情。
     */
    @PreAuthorize("@ss.hasPermi('cost:openApp:query')")
    @GetMapping("/{appId}")
    public AjaxResult getInfo(@PathVariable Long appId) {
        return success(openAppService.selectOpenAppById(appId));
    }

    /**
     * 查询开放应用可授权场景选项。
     */
    @PreAuthorize("@ss.hasPermi('cost:openApp:list')")
    @GetMapping("/sceneOptions")
    public AjaxResult sceneOptions() {
        CostScene query = new CostScene();
        query.setStatus("0");
        return success(sceneService.selectSceneOptions(query));
    }

    /**
     * 新增开放应用。
     */
    @PreAuthorize("@ss.hasPermi('cost:openApp:add')")
    @Log(title = "开放应用", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CostOpenApp app) {
        if (!openAppService.checkOpenAppCodeUnique(app)) {
            return error("新增开放应用失败，应用编码已存在");
        }
        app.setCreateBy(getUsername());
        CostOpenApp saved = openAppService.insertOpenApp(app);
        return success(buildSecretPayload(saved, "开放应用创建成功，请立即复制并交付给第三方，系统不会再次展示完整 appSecret。"));
    }

    /**
     * 修改开放应用。
     */
    @PreAuthorize("@ss.hasPermi('cost:openApp:edit')")
    @Log(title = "开放应用", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CostOpenApp app) {
        if (!openAppService.checkOpenAppCodeUnique(app)) {
            return error("修改开放应用失败，应用编码已存在");
        }
        app.setUpdateBy(getUsername());
        return toAjax(openAppService.updateOpenApp(app));
    }

    /**
     * 重置开放应用密钥。
     */
    @PreAuthorize("@ss.hasPermi('cost:openApp:resetSecret')")
    @Log(title = "开放应用", businessType = BusinessType.UPDATE)
    @PutMapping("/resetSecret/{appId}")
    public AjaxResult resetSecret(@PathVariable Long appId) {
        CostOpenApp saved = openAppService.resetOpenAppSecret(appId, getUsername());
        return success(buildSecretPayload(saved, "密钥已重置。请尽快替换第三方系统中的 appSecret，旧密钥将立即失效。"));
    }

    /**
     * 删除开放应用。
     */
    @PreAuthorize("@ss.hasPermi('cost:openApp:remove')")
    @Log(title = "开放应用", businessType = BusinessType.DELETE)
    @DeleteMapping("/{appIds}")
    public AjaxResult remove(@PathVariable Long[] appIds) {
        return toAjax(openAppService.deleteOpenAppByIds(appIds));
    }

    private Map<String, Object> buildSecretPayload(CostOpenApp app, String advice) {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("app", app);
        data.put("appSecret", app == null ? null : app.getAppSecretPlaintext());
        data.put("advice", advice);
        return data;
    }
}
